package android.pocket;

import android.pocket.IPocketCallback;

/** @hide */
interface IPocketService {

    void addCallback(IPocketCallback callback);
    void removeCallback(IPocketCallback callback);
    void onInteractiveChanged(boolean interactive);
    boolean isDeviceInPocket();

}