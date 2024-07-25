/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.systemui.statusbar.pipeline.wifi.data.repository.prod

import android.net.wifi.WifiManager
import android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.flags.Flags
import com.android.systemui.log.LogBuffer
import com.android.systemui.log.core.LogLevel
import com.android.systemui.log.table.TableLogBuffer
import com.android.systemui.log.table.logDiffsForTable
import com.android.systemui.statusbar.connectivity.WifiPickerTrackerFactory
import com.android.systemui.statusbar.pipeline.dagger.WifiTrackerLibInputLog
import com.android.systemui.statusbar.pipeline.dagger.WifiTrackerLibTableLog
import com.android.systemui.statusbar.pipeline.ims.data.model.ImsStateModel
import com.android.systemui.statusbar.pipeline.ims.data.repository.CommonImsRepository
import com.android.systemui.statusbar.pipeline.shared.data.model.DataActivityModel
import com.android.systemui.statusbar.pipeline.wifi.data.repository.WifiRepository.Companion.CARRIER_MERGED_INVALID_SUB_ID_REASON
import com.android.systemui.statusbar.pipeline.wifi.data.repository.WifiRepository.Companion.COL_NAME_IS_DEFAULT
import com.android.systemui.statusbar.pipeline.wifi.data.repository.WifiRepository.Companion.COL_NAME_IS_ENABLED
import com.android.systemui.statusbar.pipeline.wifi.data.repository.WifiRepositoryViaTrackerLibDagger
import com.android.systemui.statusbar.pipeline.wifi.data.repository.prod.WifiRepositoryImpl.Companion.WIFI_NETWORK_DEFAULT
import com.android.systemui.statusbar.pipeline.wifi.data.repository.prod.WifiRepositoryImpl.Companion.WIFI_STATE_DEFAULT
import com.android.systemui.statusbar.pipeline.wifi.shared.model.WifiNetworkModel
import com.android.systemui.statusbar.pipeline.wifi.shared.model.WifiNetworkModel.Inactive.toHotspotDeviceType
import com.android.systemui.statusbar.pipeline.wifi.shared.model.WifiScanEntry
import com.android.wifitrackerlib.HotspotNetworkEntry
import com.android.wifitrackerlib.MergedCarrierEntry
import com.android.wifitrackerlib.WifiEntry
import com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MAX
import com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MIN
import com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_UNREACHABLE
import com.android.wifitrackerlib.WifiPickerTracker
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * An implementation of [WifiRepository] that uses [com.android.wifitrackerlib] as the source of
 * truth for wifi information.
 *
 * Serves as a possible replacement for [WifiRepositoryImpl]. See b/292534484.
 */
@SysUISingleton
class WifiRepositoryViaTrackerLib
@Inject
constructor(
    featureFlags: FeatureFlags,
    @Application private val scope: CoroutineScope,
    @Main private val mainExecutor: Executor,
    @Background private val bgDispatcher: CoroutineDispatcher,
    private val wifiPickerTrackerFactory: WifiPickerTrackerFactory,
    private val wifiManager: WifiManager,
    @WifiTrackerLibInputLog private val inputLogger: LogBuffer,
    @WifiTrackerLibTableLog private val wifiTrackerLibTableLogBuffer: TableLogBuffer,
    private val commonImsRepo: CommonImsRepository,
) : WifiRepositoryViaTrackerLibDagger, LifecycleOwner {

    override val lifecycle =
        LifecycleRegistry(this).also {
            mainExecutor.execute { it.currentState = Lifecycle.State.CREATED }
        }

    private val isInstantTetherEnabled = featureFlags.isEnabled(Flags.INSTANT_TETHER)

    private var wifiPickerTracker: WifiPickerTracker? = null

    private val wifiPickerTrackerInfo: StateFlow<WifiPickerTrackerInfo> = run {
        var current =
            WifiPickerTrackerInfo(
                state = WIFI_STATE_DEFAULT,
                isDefault = false,
                primaryNetwork = WIFI_NETWORK_DEFAULT,
                secondaryNetworks = emptyList(),
            )
        callbackFlow {
                val callback =
                    object : WifiPickerTracker.WifiPickerTrackerCallback {
                        override fun onWifiEntriesChanged() {
                            val connectedEntry = wifiPickerTracker?.connectedWifiEntry
                            logOnWifiEntriesChanged(connectedEntry)

                            val secondaryNetworks =
                                if (featureFlags.isEnabled(Flags.WIFI_SECONDARY_NETWORKS)) {
                                    val activeNetworks =
                                        wifiPickerTracker?.activeWifiEntries ?: emptyList()
                                    activeNetworks
                                        .filter { it != connectedEntry && !it.isPrimaryNetwork }
                                        .map { it.toWifiNetworkModel() }
                                } else {
                                    emptyList()
                                }

                            // [WifiPickerTracker.connectedWifiEntry] will return the same instance
                            // but with updated internals. For example, when its validation status
                            // changes from false to true, the same instance is re-used but with the
                            // validated field updated.
                            //
                            // Because it's the same instance, the flow won't re-emit the value
                            // (even though the internals have changed). So, we need to transform it
                            // into our internal model immediately. [toWifiNetworkModel] always
                            // returns a new instance, so the flow is guaranteed to emit.
                            send(
                                newPrimaryNetwork = connectedEntry?.toPrimaryWifiNetworkModel()
                                        ?: WIFI_NETWORK_DEFAULT,
                                newSecondaryNetworks = secondaryNetworks,
                                newIsDefault = connectedEntry?.isDefaultNetwork ?: false,
                            )
                        }

                        override fun onWifiStateChanged() {
                            val state = wifiPickerTracker?.wifiState
                            logOnWifiStateChanged(state)
                            send(newState = state ?: WIFI_STATE_DEFAULT)
                        }

                        override fun onNumSavedNetworksChanged() {}

                        override fun onNumSavedSubscriptionsChanged() {}

                        private fun send(
                            newState: Int = current.state,
                            newIsDefault: Boolean = current.isDefault,
                            newPrimaryNetwork: WifiNetworkModel = current.primaryNetwork,
                            newSecondaryNetworks: List<WifiNetworkModel> =
                                current.secondaryNetworks,
                        ) {
                            val new =
                                WifiPickerTrackerInfo(
                                    newState,
                                    newIsDefault,
                                    newPrimaryNetwork,
                                    newSecondaryNetworks,
                                )
                            current = new
                            trySend(new)
                        }
                    }

                wifiPickerTracker =
                    wifiPickerTrackerFactory.create(lifecycle, callback).apply {
                        // By default, [WifiPickerTracker] will scan to see all available wifi
                        // networks in the area. Because SysUI only needs to display the
                        // **connected** network, we don't need scans to be running (and in fact,
                        // running scans is costly and should be avoided whenever possible).
                        this?.disableScanning()
                    }
                // The lifecycle must be STARTED in order for the callback to receive events.
                mainExecutor.execute { lifecycle.currentState = Lifecycle.State.STARTED }
                awaitClose {
                    mainExecutor.execute { lifecycle.currentState = Lifecycle.State.CREATED }
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, current)
    }

    override val isWifiEnabled: StateFlow<Boolean> =
        wifiPickerTrackerInfo
            .map { it.state == WifiManager.WIFI_STATE_ENABLED }
            .distinctUntilChanged()
            .logDiffsForTable(
                wifiTrackerLibTableLogBuffer,
                columnPrefix = "",
                columnName = COL_NAME_IS_ENABLED,
                initialValue = false,
            )
            .stateIn(scope, SharingStarted.Eagerly, false)

    override val wifiNetwork: StateFlow<WifiNetworkModel> =
        wifiPickerTrackerInfo
            .map { it.primaryNetwork }
            .distinctUntilChanged()
            .logDiffsForTable(
                wifiTrackerLibTableLogBuffer,
                columnPrefix = "",
                initialValue = WIFI_NETWORK_DEFAULT,
            )
            .stateIn(scope, SharingStarted.Eagerly, WIFI_NETWORK_DEFAULT)

    override val secondaryNetworks: StateFlow<List<WifiNetworkModel>> =
        wifiPickerTrackerInfo
            .map { it.secondaryNetworks }
            .distinctUntilChanged()
            .logDiffsForTable(
                wifiTrackerLibTableLogBuffer,
                columnPrefix = "",
                columnName = "secondaryNetworks",
                initialValue = emptyList(),
            )
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /**
     * Converts WifiTrackerLib's [WifiEntry] into our internal model only if the entry is the
     * primary network. Returns an inactive network if it's not primary.
     */
    private fun WifiEntry.toPrimaryWifiNetworkModel(): WifiNetworkModel {
        return if (!this.isPrimaryNetwork) {
            WIFI_NETWORK_DEFAULT
        } else {
            this.toWifiNetworkModel()
        }
    }

    /** Converts WifiTrackerLib's [WifiEntry] into our internal model. */
    private fun WifiEntry.toWifiNetworkModel(): WifiNetworkModel {
        return if (this is MergedCarrierEntry) {
            this.convertCarrierMergedToModel()
        } else {
            this.convertNormalToModel()
        }
    }

    private fun MergedCarrierEntry.convertCarrierMergedToModel(): WifiNetworkModel {
        return if (this.subscriptionId == INVALID_SUBSCRIPTION_ID) {
            WifiNetworkModel.Invalid(CARRIER_MERGED_INVALID_SUB_ID_REASON)
        } else {
            WifiNetworkModel.CarrierMerged(
                networkId = NETWORK_ID,
                subscriptionId = this.subscriptionId,
                level = this.level,
                // WifiManager APIs to calculate the signal level start from 0, so
                // maxSignalLevel + 1 represents the total level buckets count.
                numberOfLevels = wifiManager.maxSignalLevel + 1,
            )
        }
    }

    private fun WifiEntry.convertNormalToModel(): WifiNetworkModel {
        if (this.level == WIFI_LEVEL_UNREACHABLE || this.level !in WIFI_LEVEL_MIN..WIFI_LEVEL_MAX) {
            // If our level means the network is unreachable or the level is otherwise invalid, we
            // don't have an active network.
            return WifiNetworkModel.Inactive
        }

        val hotspotDeviceType =
            if (isInstantTetherEnabled && this is HotspotNetworkEntry) {
                this.deviceType.toHotspotDeviceType()
            } else {
                WifiNetworkModel.HotspotDeviceType.NONE
            }

        return WifiNetworkModel.Active(
            networkId = NETWORK_ID,
            isValidated = this.hasInternetAccess(),
            level = this.level,
            ssid = this.title,
            hotspotDeviceType = hotspotDeviceType,
            // With WifiTrackerLib, [WifiEntry.title] will appropriately fetch the  SSID for
            // typical wifi networks *and* passpoint/OSU APs. So, the AP-specific values can
            // always be false/null in this repository.
            // TODO(b/292534484): Remove these fields from the wifi network model once this
            //  repository is fully enabled.
            isPasspointAccessPoint = false,
            isOnlineSignUpForPasspointAccessPoint = false,
            passpointProviderFriendlyName = null,
        )
    }

    override val isWifiDefault: StateFlow<Boolean> =
        wifiPickerTrackerInfo
            .map { it.isDefault }
            .distinctUntilChanged()
            .logDiffsForTable(
                wifiTrackerLibTableLogBuffer,
                columnPrefix = "",
                columnName = COL_NAME_IS_DEFAULT,
                initialValue = false,
            )
            .stateIn(scope, SharingStarted.Eagerly, false)

    override val wifiActivity: StateFlow<DataActivityModel> =
        WifiRepositoryHelper.createActivityFlow(
            wifiManager,
            mainExecutor,
            scope,
            wifiTrackerLibTableLogBuffer,
            this::logActivity,
        )

    override val wifiScanResults: StateFlow<List<WifiScanEntry>> =
        WifiRepositoryHelper.createNetworkScanFlow(
            wifiManager,
            scope,
            bgDispatcher,
            this::logScanResults,
        )

    override val imsStates: StateFlow<List<ImsStateModel>> = commonImsRepo.imsStates

    private fun logOnWifiEntriesChanged(connectedEntry: WifiEntry?) {
        inputLogger.log(
            TAG,
            LogLevel.DEBUG,
            { str1 = connectedEntry.toString() },
            { "onWifiEntriesChanged. ConnectedEntry=$str1" },
        )
    }

    private fun logOnWifiStateChanged(state: Int?) {
        inputLogger.log(
            TAG,
            LogLevel.DEBUG,
            { int1 = state ?: -1 },
            { "onWifiStateChanged. State=${if (int1 == -1) null else int1}" },
        )
    }

    private fun logActivity(activity: String) {
        inputLogger.log(TAG, LogLevel.DEBUG, { str1 = activity }, { "onActivityChanged: $str1" })
    }

    private fun logScanResults() =
        inputLogger.log(TAG, LogLevel.DEBUG, {}, { "onScanResultsAvailable" })

    /**
     * Data class storing all the information fetched from [WifiPickerTracker].
     *
     * Used so that we only register a single callback on [WifiPickerTracker].
     */
    data class WifiPickerTrackerInfo(
        /** The current wifi state. See [WifiManager.getWifiState]. */
        val state: Int,
        /** True if wifi is currently the default connection and false otherwise. */
        val isDefault: Boolean,
        /** The currently primary wifi network. */
        val primaryNetwork: WifiNetworkModel,
        /** The current secondary network(s), if any. Specifically excludes the primary network. */
        val secondaryNetworks: List<WifiNetworkModel>
    )

    @SysUISingleton
    class Factory
    @Inject
    constructor(
        private val featureFlags: FeatureFlags,
        @Application private val scope: CoroutineScope,
        @Main private val mainExecutor: Executor,
        @Background private val bgDispatcher: CoroutineDispatcher,
        private val wifiPickerTrackerFactory: WifiPickerTrackerFactory,
        @WifiTrackerLibInputLog private val inputLogger: LogBuffer,
        @WifiTrackerLibTableLog private val wifiTrackerLibTableLogBuffer: TableLogBuffer,
        private val commonImsRepository: CommonImsRepository,
    ) {
        fun create(wifiManager: WifiManager): WifiRepositoryViaTrackerLib {
            return WifiRepositoryViaTrackerLib(
                featureFlags,
                scope,
                mainExecutor,
                bgDispatcher,
                wifiPickerTrackerFactory,
                wifiManager,
                inputLogger,
                wifiTrackerLibTableLogBuffer,
                commonImsRepository,
            )
        }
    }

    companion object {
        private const val TAG = "WifiTrackerLibInputLog"

        /**
         * [WifiNetworkModel.Active.networkId] is only used at the repository layer. It's used by
         * [WifiRepositoryImpl], which tracks the ID in order to correctly apply the framework
         * callbacks within the repository.
         *
         * Since this class does not need to manually apply framework callbacks and since the
         * network ID is not used beyond the repository, it's safe to use an invalid ID in this
         * repository.
         *
         * The [WifiNetworkModel.Active.networkId] field should be deleted once we've fully migrated
         * to [WifiRepositoryViaTrackerLib].
         */
        private const val NETWORK_ID = -1
    }
}
