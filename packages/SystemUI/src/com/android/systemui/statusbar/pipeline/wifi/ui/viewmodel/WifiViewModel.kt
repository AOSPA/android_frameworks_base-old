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

package com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel

import android.content.Context
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.log.table.TableLogBuffer
import com.android.systemui.log.table.logDiffsForTable
import com.android.systemui.statusbar.pipeline.airplane.ui.viewmodel.AirplaneModeViewModel
import com.android.systemui.statusbar.pipeline.dagger.StatusBarPipelineModule.Companion.FIRST_MOBILE_SUB_SHOWING_NETWORK_TYPE_ICON
import com.android.systemui.statusbar.pipeline.dagger.WifiTableLog
import com.android.systemui.statusbar.pipeline.shared.ConnectivityConstants
import com.android.systemui.statusbar.pipeline.shared.data.model.DataActivityModel
import com.android.systemui.statusbar.pipeline.wifi.domain.interactor.WifiInteractor
import com.android.systemui.statusbar.pipeline.wifi.shared.WifiConstants
import com.android.systemui.statusbar.pipeline.wifi.shared.model.WifiNetworkModel
import com.android.systemui.statusbar.pipeline.wifi.ui.model.VoWifiIcon
import com.android.systemui.statusbar.pipeline.wifi.ui.model.WifiIcon
import com.android.systemui.statusbar.pipeline.wifi.ui.model.icon
import java.util.function.Supplier
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Models the UI state for the status bar wifi icon.
 *
 * This is a singleton so that we don't have duplicate logs and should *not* be used directly to
 * control views. Instead, use an instance of [LocationBasedWifiViewModel]. See
 * [LocationBasedWifiViewModel.viewModelForLocation].
 */
@SysUISingleton
class WifiViewModel
@Inject
constructor(
    airplaneModeViewModel: AirplaneModeViewModel,
    // TODO(b/238425913): The wifi icon shouldn't need to consume mobile information. A
    //  container-level view model should do the work instead.
    @Named(FIRST_MOBILE_SUB_SHOWING_NETWORK_TYPE_ICON)
    shouldShowSignalSpacerProvider: Supplier<Flow<Boolean>>,
    connectivityConstants: ConnectivityConstants,
    private val context: Context,
    @WifiTableLog wifiTableLogBuffer: TableLogBuffer,
    interactor: WifiInteractor,
    @Application private val scope: CoroutineScope,
    wifiConstants: WifiConstants,
) : WifiViewModelCommon {
    /** Returns the icon to use based on the given network. */
    private fun WifiNetworkModel.icon(): WifiIcon {
        return WifiIcon.fromModel(this, context)
    }

    override val wifiIcon: StateFlow<WifiIcon> =
        combine(
                interactor.isEnabled,
                interactor.isDefault,
                interactor.isForceHidden,
                interactor.wifiNetwork,
            ) { isEnabled, isDefault, isForceHidden, wifiNetwork ->
                if (!isEnabled || isForceHidden || wifiNetwork is WifiNetworkModel.CarrierMerged) {
                    return@combine WifiIcon.Hidden
                }

                val icon = wifiNetwork.icon()

                return@combine when {
                    isDefault -> icon
                    wifiConstants.alwaysShowIconIfEnabled -> icon
                    !connectivityConstants.hasDataCapabilities -> icon
                    // See b/272509965: Even if we have an active and validated wifi network, we
                    // don't want to show the icon if wifi isn't the default network.
                    else -> WifiIcon.Hidden
                }
            }
            .logDiffsForTable(
                wifiTableLogBuffer,
                columnPrefix = "",
                initialValue = WifiIcon.Hidden,
            )
            .stateIn(
                scope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = WifiIcon.Hidden
            )

    /** The wifi activity status. Null if we shouldn't display the activity status. */
    private val activity: Flow<DataActivityModel> = run {
        val default = DataActivityModel(hasActivityIn = false, hasActivityOut = false)
        if (!connectivityConstants.shouldShowActivityConfig) {
                flowOf(default)
            } else {
                combine(interactor.activity, interactor.ssid) { activity, ssid ->
                    when (ssid) {
                        null -> default
                        else -> activity
                    }
                }
            }
            .distinctUntilChanged()
            .logDiffsForTable(
                wifiTableLogBuffer,
                columnPrefix = "VM.activity",
                initialValue = default,
            )
            .stateIn(scope, started = SharingStarted.WhileSubscribed(), initialValue = default)
    }

    override val isActivityInViewVisible: Flow<Boolean> =
        activity
            .map { it.hasActivityIn }
            .stateIn(scope, started = SharingStarted.WhileSubscribed(), initialValue = false)

    override val isActivityOutViewVisible: Flow<Boolean> =
        activity
            .map { it.hasActivityOut }
            .stateIn(scope, started = SharingStarted.WhileSubscribed(), initialValue = false)

    override val isActivityContainerVisible: Flow<Boolean> =
        combine(isActivityInViewVisible, isActivityOutViewVisible) { activityIn, activityOut ->
                activityIn || activityOut
            }
            .stateIn(scope, started = SharingStarted.WhileSubscribed(), initialValue = false)

    // TODO(b/238425913): It isn't ideal for the wifi icon to need to know about whether the
    //  airplane icon is visible. Instead, we should have a parent StatusBarSystemIconsViewModel
    //  that appropriately knows about both icons and sets the padding appropriately.
    override val isAirplaneSpacerVisible: Flow<Boolean> =
        airplaneModeViewModel.isAirplaneModeIconVisible

    override val isSignalSpacerVisible: Flow<Boolean> = shouldShowSignalSpacerProvider.get()

    override val voWifiIcon: Flow<VoWifiIcon> =
        combine(
                interactor.voWifiState,
                interactor.isVoWifiForceHidden
            ) { state, isHidden ->
                // If it's force hidden, just hide.
                // Otherwise follow VoWifi state
                if (isHidden) VoWifiIcon.Hidden else state.icon
            }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.WhileSubscribed(), VoWifiIcon.Hidden)
}
