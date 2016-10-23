package android.pocket;

import android.pocket.PocketManager.PocketCallback;

/** @hide */
interface IPocketService {

    void addCallback(PocketCallback callback);
    void removeCallback(PocketCallback callback);
    void onDeviceInteractiveChanged(boolean interactive);
    boolean isDeviceInPocket();

}