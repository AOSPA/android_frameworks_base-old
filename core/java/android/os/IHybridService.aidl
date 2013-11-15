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

import andoird.os.HybirdProp;

/**
 * System private API for talking with the Hybrid service.
 *
 * {@hide}
 */
interface IHybridService {

    String getPackageName();
    void setPackageName(String name);

    HybirdProp getPackageNameForResult(String packageName);

    boolean getActive();
    void setActive(boolean active);

    int getLayout();
    void setLayout(int layout);

    int getDpi();
    void setDpi(int dpi);

    int getStatusBarColor();
    void setStatusBarColor(int color);

    int getNavBarColor();
    void setNavBarColor(int color);

    int getNavBarButtonColor();
    void setNavBarButonColor(int color);
}


