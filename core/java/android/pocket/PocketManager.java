package android.pocket;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

/** @hide */
public class PocketManager {

    private static final String TAG = PocketManager.class.getSimpleName();

    private Context mContext;
    private IPocketService mService;

    /** @hide */
    public PocketManager(Context context, IPocketService service) {
        mContext = context;
        mService = service;
        if (mService == null) {
            Slog.v(TAG, "PocketService was null");
        }
    }

    /** @hide */
    public void addCallback(final IPocketServiceCallback callback) {
        if (mService != null) try {
            mService.addCallback(callback);
        } catch (RemoteException e1) {
            Log.w(TAG, "Remote exception in addCallback: ", e1);
            if (callback != null){
                try {
                    callback.onPocketStateChanged(false);
                } catch (RemoteException e2) {
                    Log.w(TAG, "Remote exception in callback.onPocketStateChanged: ", e2);
                }
            }
        }
    }

    /** @hide */
    public void removeCallback(final IPocketServiceCallback callback) {
        if (mService != null) try {
            mService.removeCallback(callback);
        } catch (RemoteException e1) {
            Log.w(TAG, "Remote exception in addCallback: ", e1);
            if (callback != null){
                try {
                    callback.onPocketStateChanged(false);
                } catch (RemoteException e2) {
                    Log.w(TAG, "Remote exception in callback.onPocketStateChanged: ", e2);
                }
            }
        }
    }

    /** @hide */
    public void onDeviceInteractiveChanged(boolean interactive) {
        if (mService != null) try {
            mService.onDeviceInteractiveChanged(interactive);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
        }
    }

    /** @hide */
    public boolean isDeviceInPocket() {
        if (mService != null) try {
            return mService.isDeviceInPocket();
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
        }
        return false;
    }

}
