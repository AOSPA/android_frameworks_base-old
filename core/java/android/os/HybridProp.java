/*
* Copyright (C) 2013 ParanoidAndroid project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package android.os;

import android.os.HybridProp;

/**
 * Object to hold Hybrid properties.
 * Implments parcable in order to be marshaled acorss binder.
 * @hide
 */
public class HybridProp implements Parcelable {

    public String packageName;
    public boolean active;
    public int dpi;
    public int layout;
    public int navBarColor;
    public int navBarButtonColor;
    public int navBarGlowColor;
    public int statusBarColor;
    public int statusBarIconColor;

    /**
     * Creates a new HybridProp with values set to zero
     * @param name the packageName for the property
     * @param state active state
     * @hide
     */
    public HybridProp(String name, boolean state) {
        packageName = name;
        active = state;
    }

    /**
     * Creates a new HybridProp with values set to zero
     * @param name the packageName for the property
     * @hide
     */
    public HybridProp(String name) {
        packageName = name;
    }

    private HybridProp(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeInt(active ? 1 : 0);
        dest.writeInt(dpi);
        dest.writeInt(layout);
        dest.writeInt(navBarColor);
        dest.writeInt(navBarButtonColor);
        dest.writeInt(navBarGlowColor);
        dest.writeInt(statusBarColor);
        dest.writeInt(statusBarIconColor);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void readFromParcel(Parcel in) {
        packageName = in.readString();
        active = in.readInt() == 1;
        dpi = in.readInt();
        layout = in.readInt();
        navBarColor = in.readInt();
        navBarButtonColor = in.readInt();
        navBarGlowColor = in.readInt();
        statusBarColor = in.readInt();
        statusBarIconColor = in.readInt();
    }

    /** @hide */
    public static final Parcelable.Creator<HybridProp> CREATOR =
        new Parcelable.Creator<HybridProp>() {

        public HybridProp createFromParcel(Parcel in) {
            return new HybridProp(in);
        }

        public HybridProp[] newArray(int size) {
            return new HybridProp[size];
        }
    };

    @Override
    public String toString() {
        return packageName + " active: " + active + ", dpi: " + dpi
        + ", layout: " + layout + ", navBarColor: " + navBarColor
		+ ", navBarGlowColor: " + navBarGlowColor + ", NavbuttonColor: "
		+ navBarButtonColor + ", statusBarColor: " + statusBarColor
		+ ", statusBarIconColor: " + statusBarIconColor;
    }
}
