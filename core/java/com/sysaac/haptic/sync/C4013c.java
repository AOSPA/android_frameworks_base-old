package com.sysaac.haptic.sync;

import android.os.Parcel;
import android.os.Parcelable;

final class C4013c implements Parcelable.Creator<C4012b> {
    @Override
    public C4012b createFromParcel(Parcel parcel) {
        return new C4012b(parcel);
    }

    @Override
    public C4012b[] newArray(int i) {
        return new C4012b[i];
    }
}
