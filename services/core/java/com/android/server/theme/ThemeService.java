package com.android.server.theme;

import android.app.theme.IThemeService;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.android.internal.util.ConcurrentUtils;

import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

public class ThemeService extends SystemService {

    private static final String TAG = ThemeService.class.getSimpleName();

    private Context mContext;
    private IOverlayManager mOverlayManager;

    private ThemeHelper mThemeHelper;

    private Future<?> mInitCompleteSignal;

    public ThemeService(Context context) {
        mContext = context;
        mInitCompleteSignal = SystemServerInitThreadPool.get().submit(() -> {
            mOverlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));
            mThemeHelper = new ThemeHelper(mContext);

            publishBinderService(Context.OVERLAY_SERVICE, mService);
            publishLocalService(ThemeService.class, this);
        }, "Init ThemeService");
    }

    @Override
    public void onStart() {
        // Intentionally left empty.
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            ConcurrentUtils.waitForFutureNoInterrupt(mInitCompleteSignal,
                    "Wait for ThemeService init");
            mInitCompleteSignal = null;
        }
    }

    private final IBinder mService = new IThemeService.Stub() {
        @Override
        public boolean updateTheme(@NonNull OverlayInfo oi, String accentColor) {
            // TODO: Move to settings
            //int userId = UserHandle.myUserId();
            //Map<String, List<OverlayInfo>> overlays = mOverlayManager.getAllOverlays(userId);
            //for (Entry<String, List<OverlayInfo>> entry : overlays.entrySet()) {
                //for (OverlayInfo oi : entry.getValue()) {
            String packageName = oi.packageName;
            if (packageName.startsWith(ThemeHelper.THEME_PACKAGE_PREFIX)) {
                File apk = mThemeHelper.buildApk(packageName, accentColor);
                if (apk != null) {
                    try {
                        // install the overlay
                        if (!mThemeHelper.install(Uri.fromFile(apk))) {
                            Log.e(TAG, "failed to install package " + apk);
                            return false;
                        }
                        // enable overlay
                        return true;//mThemeHelper.enableOverlay(
                    } catch (RemoteException e) {
                    }
                }
            }
            return false;
        }
    };
}
