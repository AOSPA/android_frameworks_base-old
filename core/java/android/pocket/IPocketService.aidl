package android.pocket;

import android.pocket.IPocketServiceCallback;

/** @hide */
interface IPocketService {

    void addCallback(IPocketServiceCallback callback);
    void removeCallback(IPocketServiceCallback callback);
    void onDeviceInteractiveChanged(boolean interactive);
    boolean isDeviceInPocket();

}