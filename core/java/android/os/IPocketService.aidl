package android.os;

import android.os.PocketManager.PocketCallback;

/** @hide */
interface IPocketService {

    void addCallback(PocketCallback callback);
    void removeCallback(PocketCallback callback);
    void onDeviceInteractiveChanged(boolean interactive);
    boolean isDeviceInPocket();

}