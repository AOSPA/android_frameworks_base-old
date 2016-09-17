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

/**
 * Object to hold Hybrid properties.
 * Implements parcelable in order to be marshaled across binder.
 * //TODO implment hashcode and equals using packageName which is guaranteed to be unique
 *
 * @hide
 */
public class HybridProp implements Parcelable {

    // Default values
    public static final int PHONE_DPI = 320;
    public static final int PHABLET_DPI = 240;
    public static final int TABLET_DPI = 180;
    public static final int PHONE_LAYOUT = 320;
    public static final int PHABLET_LAYOUT = 480;
    public static final int TABLET_LAYOUT = 720;

    public String packageName;
    public boolean active;
    public int dpi;
    public int layout;

    /**
     * Creates a new HybridProp with values set to zero
     *
     * @param name  the packageName for the property
     * @param state active state
     * @hide
     */
    public HybridProp(String name, boolean state) {
        packageName = name;
        active = state;
    }

    /**
     * Creates a new HybridProp with values set to zero
     *
     * @param name the packageName for the property
     * @hide
     */
    public HybridProp(String name) {
        packageName = name;
        active = false;
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
    }

    /**
     * @hide
     */
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
                + ", layout: " + layout;
    }

}
