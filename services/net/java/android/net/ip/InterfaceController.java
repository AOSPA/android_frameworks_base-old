/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.net.ip;

import android.net.INetd;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.OsConstants;

import java.net.InetAddress;


/**
 * Encapsulates the multiple IP configuration operations performed on an interface.
 *
 * TODO: refactor/eliminate the redundant ways to set and clear addresses.
 *
 * @hide
 */
public class InterfaceController {
    private final static boolean DBG = false;

    private final String mIfName;
    private final INetworkManagementService mNMS;
    private final INetd mNetd;
    private final SharedLog mLog;

    public InterfaceController(String ifname, INetworkManagementService nms, INetd netd,
            SharedLog log) {
        mIfName = ifname;
        mNMS = nms;
        mNetd = netd;
        mLog = log;
    }


    public boolean addAddress(LinkAddress addr) {
        return addAddress(addr.getAddress(), addr.getPrefixLength());
    }

    public boolean addAddress(InetAddress ip, int prefixLen) {
        try {
            mNetd.interfaceAddAddress(mIfName, ip.getHostAddress(), prefixLen);
        } catch (ServiceSpecificException | RemoteException e) {
            logError("failed to add %s/%d: %s", ip, prefixLen, e);
            return false;
        }
        return true;
    }

    public boolean removeAddress(InetAddress ip, int prefixLen) {
        try {
            mNetd.interfaceDelAddress(mIfName, ip.getHostAddress(), prefixLen);
        } catch (ServiceSpecificException | RemoteException e) {
            logError("failed to remove %s/%d: %s", ip, prefixLen, e);
            return false;
        }
        return true;
    }

    private void logError(String fmt, Object... args) {
        mLog.e(String.format(fmt, args));
    }
}
