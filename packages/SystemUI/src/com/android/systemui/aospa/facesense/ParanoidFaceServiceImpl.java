package com.android.systemui.aospa.facesense;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Size;
import android.util.SparseIntArray;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import vendor.pa.biometrics.face.V1_0.IFaceServiceReceiver;

public class ParanoidFaceServiceImpl extends ParanoidFaceService {

    private static final SparseIntArray ORIENTATION = new SparseIntArray();
    protected boolean isFrameOnProcess = false;
    protected Handler mAuthHandler = new Handler(getAuthHandler()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 10) {
                if (mIsAuthenticated) {
                    Log.d(TAG, "WHAT_PROCESS_FRAME: already authenticated, just drop this frame!");
                    isFrameOnProcess = false;
                    return;
                }
                Trace.beginSection("Keyguard#ProcessFrame");
                byte[] bArr = (byte[]) message.obj;
                if (!(sFaceServiceWrapper == null || mFaceUnlockHandler == null)) {
                    Log.d(TAG, "PROCESS_FRAME: auth start");
                    int authenticate = sFaceServiceWrapper.authenticate(bArr, 640, 480);
                    Log.d(TAG, "PROCESS_FRAME: auth end, result: " + authenticate);
                    if (authenticate == 0) {
                        try {
                            mIsAuthenticated = true;
                            mFaceServiceReceiver.onAcquired(0);
                            mFaceServiceReceiver.onAuthenticationSucceeded(0, mCurrentUserId);
                            mFaceUnlockHandler.removeMessages(2);
                            resetCompareFailCount();
                        } catch (RemoteException e) {
                            Log.d(TAG, "fail acquire mFaceServiceReceiver ", e);
                        }
                    } else {
                        if (authenticate == -12 || (sIsAssumeWarningAsAttack && authenticate == -13)) {
                            sCompareFailCount++;
                            Log.d(TAG, "sCompareFailCount: " + sCompareFailCount);
                        }
                        mFaceServiceReceiver.onAcquired(22);
                    }
                }
                Trace.endSection();
                isFrameOnProcess = false;
            }
        }
    };

    private CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String str) {
            super.onCameraAvailable(str);
            Log.d(TAG, "onCameraAvailable " + str);
            if (str.equalsIgnoreCase("1")) {
                Log.d(TAG, "camera 1(front camera) is available, start init");
                mFaceUnlockHandler.sendEmptyMessage(1);
                CameraManager cameraManager = (CameraManager) mContext.getSystemService("camera");
                if (cameraManager != null) {
                    cameraManager.unregisterAvailabilityCallback(super);
                }
            }
        }

        @Override
        public void onCameraUnavailable(String str) {
            super.onCameraUnavailable(str);
            Log.d(TAG, "onCameraUnavailable " + str);
            if (str.equalsIgnoreCase("1")) {
                Log.d(TAG, "camera 1(front camera) is unavailable, handle FaceAuthFailed");
                sFaceServiceWrapper.endAuthenticate();
                CameraManager cameraManager = (CameraManager) mContext.getSystemService("camera");
                if (cameraManager != null) {
                    cameraManager.unregisterAvailabilityCallback(super);
                }
            }
        }
    };

    public CameraCaptureSession mCameraCaptureSession;
    public CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private Size mPreviewSize;

    private String mCameraId;
    public boolean mCameraStarting = false;

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            if (mCameraDevice == null) {
                mCameraDevice = cameraDevice;
                mCameraStarting = false;
                Log.d(TAG, "open CameraDevice end and startPreview");
                startPreview();
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    public ParanoidFaceServiceImpl(Context context, IFaceServiceReceiver iFaceServiceReceiver) {
        super(context, iFaceServiceReceiver);
    }

    public boolean handleStartCamera() {
        Log.d(TAG, "handleStartCamera");
        if (mCameraDevice != null || mCameraStarting) {
            Log.d(TAG, "mCameraDevice already exist");
            return true;
        }
        mCameraStarting = true;
        super.sCompareFailCount = 0;
        CameraManager cameraManager = (CameraManager) super.mContext.getSystemService("camera");
        try {
            String[] cameraIdList = cameraManager.getCameraIdList();
            int length = cameraIdList.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                String idList = cameraIdList[i];
                CameraCharacteristics cameraChar = cameraManager.getCameraCharacteristics(idList);
                Integer num = (Integer) cameraChar.get(CameraCharacteristics.LENS_FACING);
                if (num != null && num.intValue() == 0) {
                    StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) cameraChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = new Size(640, 480);
                    setupImageReader();
                    mCameraId = idList;
                    break;
                }
                i++;
            }
            if (mCameraId != null) {
                if (!mCameraId.isEmpty()) {
                    cameraManager.openCamera(mCameraId, mStateCallback, super.mFaceUnlockHandler);
                    sStartTime = SystemClock.elapsedRealtime();
                    sIsAssumeWarningAsAttack = false;
                    resetCompareFailCount();
                    return true;
                }
            }
            Log.d(TAG, "mCameraId is null, handleStartCamera fail and return false this turn");
            return false;
        } catch (CameraAccessException | SecurityException e) {
            Log.d(TAG, "CameraAccessException | SecurityException", e);
            Log.d(TAG, "Reset mCameraDevice to null due to init exception");
            mImageReader = null;
            mCameraCaptureSession = null;
            mCameraDevice = null;
            mCameraStarting = false;
            return false;
        }
    }

    private void setupImageReader() {
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), 35, 3);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Log.d(TAG, "onImageAvailable start");
                if (isFrameOnProcess) {
                    Log.d(TAG, "onImageAvailable return due to FrameOnProcess");
                    return;
                }
                isFrameOnProcess = true;
                Trace.beginSection("Keyguard#onImageAvailable");
                long elapsedRealtime = SystemClock.elapsedRealtime();
                if (!sIsAssumeWarningAsAttack && elapsedRealtime - sStartTime > 1000) {
                    sIsAssumeWarningAsAttack = true;
                }
                Image image = null;
                try {
                    image = imageReader.acquireLatestImage();
                    mAuthHandler.obtainMessage(10, getDataFromImage(image, 2)).sendToTarget();
                } catch (Exception e) {
                    isFrameOnProcess = false;
                    Log.d(TAG, "Exception", e);
                } catch (Throwable th) {
                    if (image != null) {
                        image.close();
                    }
                    throw th;
                }
            }
        }, super.mFaceUnlockHandler);
    }

    static {
        ORIENTATION.append(0, 90);
        ORIENTATION.append(1, 0);
        ORIENTATION.append(2, 270);
        ORIENTATION.append(3, 180);
    }

    public void startPreview() {
        if (mCameraDevice == null || mImageReader == null) {
            Log.d(TAG, "startPreview return due to component null");
            return;
        }
        Log.d(TAG, "startPreview start");
        try {
            ArrayList config = new ArrayList();
            config.add(mImageReader.getSurface());
            final CaptureRequest.Builder createCaptureRequest = mCameraDevice.createCaptureRequest(1);
            createCaptureRequest.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(config, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        Log.d(TAG, "return when CameraCaptureSession onConfigured, mCameraDevice: " + mCameraDevice);
                        return;
                    }
                    try {
                        createCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, 4);
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(createCaptureRequest.build(), null, mFaceUnlockHandler);
                    } catch (CameraAccessException e) {
                        Log.d(TAG, "CameraAccessException: onConfigured, ", e);
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.d(TAG, "onConfigureFailed");
                }
            }, super.mFaceUnlockHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG, "CameraAccessException: createCaptureSession, ", e);
        }
        Log.d(TAG, "startPreview end");
    }

    public void handleStopCamera() {
        Log.d(TAG, "handleStopCamera: release camera (start)");
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
            Log.d(TAG, "handleStopCamera: release mImageReader");
        }

        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
            Log.d(TAG, "handleStopCamera: release mCameraCaptureSession");
        }

        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
            Log.d(TAG, "handleStopCamera: release mCameraDevice");
        }
        Log.d(TAG, "handleStopCamera: release camera (end)");
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        return format == 17 || format == 35 || format == 842094169;
    }

    public static byte[] getDataFromImage(Image image, int i) {
        Rect rect;
        int i2;
        int i3 = i;
        int i4 = 2;
        int i5 = 1;
        if (i3 != 1 && i3 != 2) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 and COLOR_FormatNV21");
        } else if (isImageFormatSupported(image)) {
            Rect cropRect = image.getCropRect();
            int format = image.getFormat();
            int width = cropRect.width();
            int height = cropRect.height();
            Image.Plane[] planes = image.getPlanes();
            int i6 = width * height;
            byte[] bArr = new byte[((ImageFormat.getBitsPerPixel(format) * i6) / 8)];
            int i7 = 0;
            byte[] bArr2 = new byte[planes[0].getRowStride()];
            int i8 = 1;
            int i9 = 0;
            int i10 = 0;
            while (i9 < planes.length) {
                if (i9 != 0) {
                    if (i9 != i5) {
                        if (i9 == i4) {
                            if (i3 == i5) {
                                i10 = (int) (((double) i6) * 1.25d);
                                i8 = i5;
                            } else if (i3 == i4) {
                                i8 = i4;
                            }
                        }
                    } else if (i3 == i5) {
                        i8 = i5;
                    } else if (i3 == i4) {
                        i10 = i6 + 1;
                        i8 = i4;
                    }
                    i10 = i6;
                } else {
                    i8 = i5;
                    i10 = i7;
                }
                ByteBuffer buffer = planes[i9].getBuffer();
                int rowStride = planes[i9].getRowStride();
                int pixelStride = planes[i9].getPixelStride();
                int i11 = i9 == 0 ? i7 : i5;
                int i12 = width >> i11;
                int i13 = height >> i11;
                int i14 = width;
                buffer.position(((cropRect.top >> i11) * rowStride) + ((cropRect.left >> i11) * pixelStride));
                int i15 = 0;
                while (i15 < i13) {
                    if (pixelStride == 1 && i8 == 1) {
                        buffer.get(bArr, i10, i12);
                        i10 += i12;
                        rect = cropRect;
                        i2 = i12;
                    } else {
                        rect = cropRect;
                        i2 = ((i12 - 1) * pixelStride) + 1;
                        buffer.get(bArr2, 0, i2);
                        int i16 = i10;
                        for (int i17 = 0; i17 < i12; i17++) {
                            bArr[i16] = bArr2[i17 * pixelStride];
                            i16 += i8;
                        }
                        i10 = i16;
                    }
                    if (i15 < i13 - 1) {
                        buffer.position((buffer.position() + rowStride) - i2);
                    }
                    i15++;
                    cropRect = rect;
                }
                i9++;
                i3 = i;
                width = i14;
                i4 = 2;
                i5 = 1;
                i7 = 0;
            }
            return bArr;
        } else {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
    }

    public void checkCameraAvailableAndStart() {
        CameraManager cameraManager = (CameraManager) super.mContext.getSystemService("camera");
        if (cameraManager != null) {
            cameraManager.registerAvailabilityCallback(mAvailabilityCallback, super.mFaceUnlockHandler);
        }
    }

    public boolean isSystemUIHoldCamera() {
        if (mCameraDevice == null && !mCameraStarting) {
            return false;
        }
        Log.d(TAG, "mCameraDevice already exist");
        return true;
    }
}
