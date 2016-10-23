package android.pocket;

/** @hide */
interface IPocketCallback {

    void onStateChanged(boolean isDeviceInPocket, int reason);

}