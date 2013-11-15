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

import android.os.Parcelable;

public class HybridProp implements Parcelable {

    public final String packageName;
    public boolean active;
    public int dpi;
    public int layout;
    public int navBarButtonColor;
    public int navBarColor;
    public int statusBarColor;
    public int statusBarIconColor;

    public HybridProp(String name) {
        packageName = name;
    }

    private HybridProp(Parcel in) {
		readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBoolean(active);
        dest.writeInt(dpi);
        dest.writeInt(layout);
    }

	@Override
	public int describeContents() {
		return 0;
	}

	private void readFromParcel(Parcel in) {
		active = in.readBoolean();
		dpi = in.readInt();
		layout = in.readInt();
	}

    public static final Parcelable.Creator<HybridProp> CREATOR =
    	new Parcelable.Creator<HybridProp>() {
            public HybridProp createFromParcel(Parcel in) {
                return new HybridProp(in);
            }

            public HybridProp createFromParcel(int size) {
                return new HybridProp[size];
            }
    };

    @Override
    public String toString() {
        return packageName + " active: " + active + ", dpi: " + dpi
            + ", layout: " + layout + ", NavbuttonColor: " + navBarButtonColor
            + ", navBarColor: " + navBarColor + ", statusBarColor: "
            + statusBarColor + ", statusBarIconColor: " + statusBarIconColor ;
    }
}
