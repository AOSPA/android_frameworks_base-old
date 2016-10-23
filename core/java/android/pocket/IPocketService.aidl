package android.pocket;

import android.pocket.PocketManager;

/** @hide */
interface IPocketService {

    void addCallback(PocketManager.PocketCallback callback);
    void removeCallback(PocketManager.PocketCallback callback);
    void onDeviceInteractiveChanged(boolean interactive);
    boolean isDeviceInPocket();

}