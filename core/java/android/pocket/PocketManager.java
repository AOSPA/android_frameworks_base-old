package android.pocket;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

/**
 * TODO> javadoc
 * @hide
 * */
public class PocketManager {

    private static final String TAG = PocketManager.class.getSimpleName();

    // TODO> javadoc
    public static final int REASON_SENSOR = 0;

    // TODO> javadoc
    public static final int REASON_ERROR = 1;

    // TODO> javadoc
    public static final int REASON_RESET = 2;

    private Context mContext;
    private IPocketService mService;

    /**
     * TODO> javadoc
     * @hide
     * */
    public PocketManager(Context context, IPocketService service) {
        mContext = context;
        mService = service;
        if (mService == null) {
            Slog.v(TAG, "PocketService was null");
        }
    }

    /**
     *  TODO> javadoc
     * @hide
     * */
    public void addCallback(final IPocketCallback callback) {
        if (mService != null) try {
            mService.addCallback(callback);
        } catch (RemoteException e1) {
            Log.w(TAG, "Remote exception in addCallback: ", e1);
            if (callback != null){
                try {
                    callback.onStateChanged(false, REASON_ERROR);
                } catch (RemoteException e2) {
                    Log.w(TAG, "Remote exception in callback.onPocketStateChanged: ", e2);
                }
            }
        }
    }

    /**
     * TODO> javadoc
     * @hide
     */
    public void removeCallback(final IPocketCallback callback) {
        if (mService != null) try {
            mService.removeCallback(callback);
        } catch (RemoteException e1) {
            Log.w(TAG, "Remote exception in addCallback: ", e1);
            if (callback != null){
                try {
                    callback.onStateChanged(false, REASON_ERROR);
                } catch (RemoteException e2) {
                    Log.w(TAG, "Remote exception in callback.onPocketStateChanged: ", e2);
                }
            }
        }
    }

    /**
     * TODO> javadoc
     * @hide
     * */
    public void onInteractiveChanged(boolean interactive) {
        if (mService != null) try {
            mService.onInteractiveChanged(interactive);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
        }
    }

    /**
     * TODO> javadoc
     * @hide
     * */
    public boolean isDeviceInPocket() {
        if (mService != null) try {
            return mService.isDeviceInPocket();
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
        }
        return false;
    }

}
