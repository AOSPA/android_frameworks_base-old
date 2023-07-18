/*
 * Copyright (C) 2022 The Android Open Source Project
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

/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.systemui.statusbar.pipeline.mobile.domain.interactor

import android.telephony.CellSignalStrength
import com.android.settingslib.mobile.TelephonyIcons
import com.android.systemui.log.table.TableLogBuffer
import com.android.systemui.statusbar.pipeline.mobile.data.model.MobileIconCustomizationMode
import com.android.systemui.statusbar.pipeline.mobile.data.model.NetworkNameModel
import com.android.systemui.statusbar.pipeline.mobile.data.repository.MobileConnectionRepository.Companion.DEFAULT_NUM_LEVELS
import com.android.systemui.statusbar.pipeline.mobile.domain.model.NetworkTypeIconModel
import com.android.systemui.statusbar.pipeline.shared.data.model.DataActivityModel
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMobileIconInteractor(
    override val tableLogBuffer: TableLogBuffer,
) : MobileIconInteractor {
    override val alwaysShowDataRatIcon = MutableStateFlow(false)

    override val alwaysUseCdmaLevel = MutableStateFlow(false)

    override val activity =
        MutableStateFlow(
            DataActivityModel(
                hasActivityIn = false,
                hasActivityOut = false,
            )
        )

    override val carrierNetworkChangeActive = MutableStateFlow(false)

    override val mobileIsDefault = MutableStateFlow(true)

    override val networkTypeIconGroup =
        MutableStateFlow<NetworkTypeIconModel>(
            NetworkTypeIconModel.DefaultIcon(TelephonyIcons.THREE_G)
        )

    override val networkName = MutableStateFlow(NetworkNameModel.IntentDerived("demo mode"))

    private val _isEmergencyOnly = MutableStateFlow(false)
    override val isEmergencyOnly = _isEmergencyOnly

    override val isRoaming = MutableStateFlow(false)

    private val _isFailedConnection = MutableStateFlow(false)
    override val isDefaultConnectionFailed = _isFailedConnection

    override val isDataConnected = MutableStateFlow(true)

    override val isInService = MutableStateFlow(true)

    private val _isDataEnabled = MutableStateFlow(true)
    override val isDataEnabled = _isDataEnabled

    private val _isDefaultDataEnabled = MutableStateFlow(true)
    override val isDefaultDataEnabled = _isDefaultDataEnabled

    private val _level = MutableStateFlow(CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN)
    override val level = _level

    private val _numberOfLevels = MutableStateFlow(DEFAULT_NUM_LEVELS)
    override val numberOfLevels = _numberOfLevels

    override val isForceHidden = MutableStateFlow(false)

    private val _alwaysUseRsrpLevelForLte = MutableStateFlow(false)
    override val alwaysUseRsrpLevelForLte = _alwaysUseRsrpLevelForLte

    private val _hideNoInternetState = MutableStateFlow(false)
    override val hideNoInternetState = _hideNoInternetState

    private val _networkTypeIconCustomization = MutableStateFlow(MobileIconCustomizationMode())
    override val networkTypeIconCustomization = _networkTypeIconCustomization

    private val _showVolteIcon = MutableStateFlow(false)
    override val showVolteIcon = _showVolteIcon

    private val _imsInfo = MutableStateFlow(MobileIconCustomizationMode())
    override val imsInfo = _imsInfo

    private val _showVowifiIcon = MutableStateFlow(false)
    override val showVowifiIcon = _showVowifiIcon

    private val _voWifiAvailable = MutableStateFlow(false)
    override val voWifiAvailable = _voWifiAvailable

    private val _isConnectionFailed = MutableStateFlow(false)
    override val isConnectionFailed = _isConnectionFailed

    fun setIsEmergencyOnly(emergency: Boolean) {
        _isEmergencyOnly.value = emergency
    }

    fun setIsDataEnabled(enabled: Boolean) {
        _isDataEnabled.value = enabled
    }

    fun setIsDefaultDataEnabled(disabled: Boolean) {
        _isDefaultDataEnabled.value = disabled
    }

    fun setIsFailedConnection(failed: Boolean) {
        _isFailedConnection.value = failed
    }

    fun setLevel(level: Int) {
        _level.value = level
    }

    fun setNumberOfLevels(num: Int) {
        _numberOfLevels.value = num
    }

    fun setAlwaysUseRsrpLevelForLte(alwaysUseRsrpLevelForLte: Boolean) {
        _alwaysUseRsrpLevelForLte.value = alwaysUseRsrpLevelForLte
    }

    fun setHideNoInternetState(hideNoInternetState: Boolean) {
        _hideNoInternetState.value = hideNoInternetState
    }
}
