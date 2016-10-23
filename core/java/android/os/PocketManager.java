package android.os;

import android.content.Context;
import android.util.Log;
import android.util.Slog;

public class PocketManager {

    private static final String TAG = PocketManager.class.getSimpleName();

    private Context mContext;
    private IPocketService mService;

    public interface PocketCallback {
        void onPocketStateChanged(boolean isDeviceInPocket);
    }

    public PocketManager(Context context, IPocketService service) {
        mContext = context;
        mService = service;
        if (mService == null) {
            Slog.v(TAG, "PocketService was null");
        }
    }

    public void addCallback(PocketCallback callback) {
        if (mService != null) try {
            mService.addCallback(callback);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
            if (callback != null) {
                callback.onPocketStateChanged(false);
            }
        }
    }

    public void removeCallback(PocketCallback callback) {
        if (mService != null) try {
            mService.removeCallback(callback);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
            if (callback != null) {
                callback.onPocketStateChanged(false);
            }
        }
    }

    public void onDeviceInteractiveChanged(boolean interactive) {
        if (mService != null) try {
            mService.onDeviceInteractiveChanged(interactive);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
        }
    }

    public boolean isDeviceInPocket() {
        if (mService != null) try {
            return mService.isDeviceInPocket();
        } catch (RemoteException e) {
            Log.w(TAG, "Remote exception in addCallback: ", e);
        }
        return false;
    }

}
