package com.sysaac.haptic.sync;

import android.os.Parcel;
import android.os.Parcelable;

public class C4012b implements Parcelable {
    public static final Parcelable.Creator<C4012b> CREATOR = new C4013c();

    /* renamed from: a */
    public String f1793a;

    /* renamed from: b */
    public int f1794b;

    /* renamed from: c */
    public int f1795c;

    public C4012b(Parcel parcel) {
        this.f1793a = parcel.readString();
        this.f1794b = parcel.readInt();
        this.f1795c = parcel.readInt();
    }

    public C4012b(String str, int i, int i2) {
        this.f1793a = str;
        this.f1794b = i;
        this.f1795c = i2;
    }

    @Override // android.p007os.Parcelable
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return "loop='" + this.f1794b + "',interval='" + this.f1795c + "'," + this.f1793a;
    }

    @Override // android.p007os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.f1793a);
        parcel.writeInt(this.f1794b);
        parcel.writeInt(this.f1795c);
    }
}
