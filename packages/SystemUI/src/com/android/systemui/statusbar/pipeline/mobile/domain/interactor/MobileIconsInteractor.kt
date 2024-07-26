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

import android.content.Context
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import com.android.settingslib.SignalIcon.MobileIconGroup
import com.android.settingslib.mobile.TelephonyIcons
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.log.table.TableLogBuffer
import com.android.systemui.log.table.logDiffsForTable
import com.android.systemui.statusbar.pipeline.dagger.MobileSummaryLog
import com.android.systemui.statusbar.pipeline.ims.data.repository.CommonImsRepository
import com.android.systemui.statusbar.pipeline.mobile.data.model.SubscriptionModel
import com.android.systemui.statusbar.pipeline.mobile.data.repository.MobileConnectionRepository
import com.android.systemui.statusbar.pipeline.mobile.data.repository.MobileConnectionsRepository
import com.android.systemui.statusbar.pipeline.mobile.data.repository.UserSetupRepository
import com.android.systemui.statusbar.pipeline.shared.data.model.ConnectivitySlot
import com.android.systemui.statusbar.pipeline.mobile.data.model.MobileIconCustomizationMode
import com.android.systemui.statusbar.pipeline.shared.data.repository.ConnectivityRepository
import com.android.systemui.util.CarrierConfigTracker
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/**
 * Business layer logic for the set of mobile subscription icons.
 *
 * This interactor represents known set of mobile subscriptions (represented by [SubscriptionInfo]).
 * The list of subscriptions is filtered based on the opportunistic flags on the infos.
 *
 * It provides the default mapping between the telephony display info and the icon group that
 * represents each RAT (LTE, 3G, etc.), as well as can produce an interactor for each individual
 * icon
 */
interface MobileIconsInteractor {
    /** See [MobileConnectionsRepository.mobileIsDefault]. */
    val mobileIsDefault: StateFlow<Boolean>

    /** List of subscriptions, potentially filtered for CBRS */
    val filteredSubscriptions: Flow<List<SubscriptionModel>>

    /** True if the active mobile data subscription has data enabled */
    val activeDataConnectionHasDataEnabled: StateFlow<Boolean>

    /**
     * Flow providing a reference to the Interactor for the active data subId. This represents the
     * [MobileConnectionInteractor] responsible for the active data connection, if any.
     */
    val activeDataIconInteractor: StateFlow<MobileIconInteractor?>

    /** True if the RAT icon should always be displayed and false otherwise. */
    val alwaysShowDataRatIcon: StateFlow<Boolean>

    /** True if the CDMA level should be preferred over the primary level. */
    val alwaysUseCdmaLevel: StateFlow<Boolean>

    /** True if there is only one active subscription. */
    val isSingleCarrier: StateFlow<Boolean>

    /** The icon mapping from network type to [MobileIconGroup] for the default subscription */
    val defaultMobileIconMapping: StateFlow<Map<String, MobileIconGroup>>

    /** Fallback [MobileIconGroup] in the case where there is no icon in the mapping */
    val defaultMobileIconGroup: StateFlow<MobileIconGroup>

    /** True only if the default network is mobile, and validation also failed */
    val isDefaultConnectionFailed: StateFlow<Boolean>

    /** True once the user has been set up */
    val isUserSetup: StateFlow<Boolean>

    /** True if we're configured to force-hide the mobile icons and false otherwise. */
    val isForceHidden: Flow<Boolean>

    /** True if we're configured to force-hide the roaming and false otherwise. */
    val isRoamingForceHidden: Flow<Boolean>

    /** True if we're configured to force-hide the hd (VoLTE/VoNR) and false otherwise. */
    val isMobileHdForceHidden: Flow<Boolean>

    /** True if we're configured to force-hide the hd (VoLTE/VoNR) and false otherwise. */
    val isVoWifiForceHidden: Flow<Boolean>

    /**
     * Vends out a [MobileIconInteractor] tracking the [MobileConnectionRepository] for the given
     * subId.
     */

    /** True if the LTE rsrp should be preferred over the primary level. */
    val alwaysUseRsrpLevelForLte: StateFlow<Boolean>

    /** True if the no internet icon should be hidden.  */
    val hideNoInternetState: StateFlow<Boolean>

    val networkTypeIconCustomization: StateFlow<MobileIconCustomizationMode>

    val showVolteIcon: StateFlow<Boolean>

    val showVowifiIcon: StateFlow<Boolean>

    fun getMobileConnectionInteractorForSubId(subId: Int): MobileIconInteractor
}

@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalCoroutinesApi::class)
@SysUISingleton
class MobileIconsInteractorImpl
@Inject
constructor(
    private val mobileConnectionsRepo: MobileConnectionsRepository,
    private val carrierConfigTracker: CarrierConfigTracker,
    @MobileSummaryLog private val tableLogger: TableLogBuffer,
    connectivityRepository: ConnectivityRepository,
    userSetupRepo: UserSetupRepository,
    commonImsRepo: CommonImsRepository,
    @Application private val scope: CoroutineScope,
    private val context: Context,
) : MobileIconsInteractor {

    // Weak reference lookup for created interactors
    private val reuseCache = mutableMapOf<Int, WeakReference<MobileIconInteractor>>()

    override val mobileIsDefault =
        combine(
                mobileConnectionsRepo.mobileIsDefault,
                mobileConnectionsRepo.hasCarrierMergedConnection,
            ) { mobileIsDefault, hasCarrierMergedConnection ->
                // Because carrier merged networks are displayed as mobile networks, they're part of
                // the `isDefault` calculation. See b/272586234.
                mobileIsDefault || hasCarrierMergedConnection
            }
            .logDiffsForTable(
                tableLogger,
                LOGGING_PREFIX,
                columnName = "mobileIsDefault",
                initialValue = false,
            )
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val activeDataConnectionHasDataEnabled: StateFlow<Boolean> =
        mobileConnectionsRepo.activeMobileDataRepository
            .flatMapLatest { it?.dataEnabled ?: flowOf(false) }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val activeDataIconInteractor: StateFlow<MobileIconInteractor?> =
        mobileConnectionsRepo.activeMobileDataSubscriptionId
            .mapLatest {
                if (it != null) {
                    getMobileConnectionInteractorForSubId(it)
                } else {
                    null
                }
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), null)

    private val unfilteredSubscriptions: Flow<List<SubscriptionModel>> =
        mobileConnectionsRepo.subscriptions

    /**
     * Generally, SystemUI wants to show iconography for each subscription that is listed by
     * [SubscriptionManager]. However, in the case of opportunistic subscriptions, we want to only
     * show a single representation of the pair of subscriptions. The docs define opportunistic as:
     *
     * "A subscription is opportunistic (if) the network it connects to has limited coverage"
     * https://developer.android.com/reference/android/telephony/SubscriptionManager#setOpportunistic(boolean,%20int)
     *
     * In the case of opportunistic networks (typically CBRS), we will filter out one of the
     * subscriptions based on
     * [CarrierConfigManager.KEY_ALWAYS_SHOW_PRIMARY_SIGNAL_BAR_IN_OPPORTUNISTIC_NETWORK_BOOLEAN],
     * and by checking which subscription is opportunistic, or which one is active.
     */
    override val filteredSubscriptions: Flow<List<SubscriptionModel>> =
        combine(
                unfilteredSubscriptions,
                mobileConnectionsRepo.activeMobileDataSubscriptionId,
                connectivityRepository.vcnSubId,
            ) { unfilteredSubs, activeId, vcnSubId ->
                // Based on the old logic,
                if (unfilteredSubs.size != 2) {
                    return@combine unfilteredSubs
                }

                val info1 = unfilteredSubs[0]
                val info2 = unfilteredSubs[1]

                // Filtering only applies to subscriptions in the same group
                if (info1.groupUuid == null || info1.groupUuid != info2.groupUuid) {
                    return@combine unfilteredSubs
                }

                // If both subscriptions are primary, show both
                if (!info1.isOpportunistic && !info2.isOpportunistic) {
                    return@combine unfilteredSubs
                }

                // NOTE: at this point, we are now returning a single SubscriptionInfo

                // If carrier required, always show the icon of the primary subscription.
                // Otherwise, show whichever subscription is currently active for internet.
                if (carrierConfigTracker.alwaysShowPrimarySignalBarInOpportunisticNetworkDefault) {
                    // return the non-opportunistic info
                    return@combine if (info1.isOpportunistic) listOf(info2) else listOf(info1)
                } else {
                    // It's possible for the subId of the VCN to disagree with the active subId in
                    // cases where the system has tried to switch but found no connection. In these
                    // scenarios, VCN will always have the subId that we want to use, so use that
                    // value instead of the activeId reported by telephony
                    val subIdToKeep = vcnSubId ?: activeId

                    return@combine if (info1.subscriptionId == subIdToKeep) {
                        listOf(info1)
                    } else {
                        listOf(info2)
                    }
                }
            }
            .distinctUntilChanged()
            .logDiffsForTable(
                tableLogger,
                LOGGING_PREFIX,
                columnName = "filteredSubscriptions",
                initialValue = listOf(),
            )
            .stateIn(scope, SharingStarted.WhileSubscribed(), listOf())

    /**
     * Copied from the old pipeline. We maintain a 2s period of time where we will keep the
     * validated bit from the old active network (A) while data is changing to the new one (B).
     *
     * This condition only applies if
     * 1. A and B are in the same subscription group (e.g. for CBRS data switching) and
     * 2. A was validated before the switch
     *
     * The goal of this is to minimize the flickering in the UI of the cellular indicator
     */
    private val forcingCellularValidation =
        mobileConnectionsRepo.activeSubChangedInGroupEvent
            .filter { mobileConnectionsRepo.defaultConnectionIsValidated.value }
            .transformLatest {
                emit(true)
                delay(2000)
                emit(false)
            }
            .logDiffsForTable(
                tableLogger,
                LOGGING_PREFIX,
                columnName = "forcingValidation",
                initialValue = false,
            )
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    /**
     * Mapping from network type to [MobileIconGroup] using the config generated for the default
     * subscription Id. This mapping is the same for every subscription.
     */
    override val defaultMobileIconMapping: StateFlow<Map<String, MobileIconGroup>> =
        mobileConnectionsRepo.defaultMobileIconMapping.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            initialValue = mapOf()
        )

    override val alwaysShowDataRatIcon: StateFlow<Boolean> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { it.alwaysShowDataRatIcon }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val alwaysUseCdmaLevel: StateFlow<Boolean> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { it.alwaysShowCdmaRssi }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val isSingleCarrier: StateFlow<Boolean> =
        mobileConnectionsRepo.subscriptions
            .map { it.size == 1 }
            .logDiffsForTable(
                tableLogger,
                columnPrefix = LOGGING_PREFIX,
                columnName = "isSingleCarrier",
                initialValue = false,
            )
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    /** If there is no mapping in [defaultMobileIconMapping], then use this default icon group */
    override val defaultMobileIconGroup: StateFlow<MobileIconGroup> =
        mobileConnectionsRepo.defaultMobileIconGroup.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            initialValue = TelephonyIcons.G
        )

    /**
     * We want to show an error state when cellular has actually failed to validate, but not if some
     * other transport type is active, because then we expect there not to be validation.
     */
    override val isDefaultConnectionFailed: StateFlow<Boolean> =
        combine(
                mobileIsDefault,
                mobileConnectionsRepo.defaultConnectionIsValidated,
                forcingCellularValidation,
            ) { mobileIsDefault, defaultConnectionIsValidated, forcingCellularValidation ->
                when {
                    !mobileIsDefault -> false
                    forcingCellularValidation -> false
                    else -> !defaultConnectionIsValidated
                }
            }
            .logDiffsForTable(
                tableLogger,
                LOGGING_PREFIX,
                columnName = "isDefaultConnectionFailed",
                initialValue = false,
            )
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val isUserSetup: StateFlow<Boolean> = userSetupRepo.isUserSetupFlow

    override val isForceHidden: Flow<Boolean> =
        connectivityRepository.forceHiddenSlots
            .map { it.contains(ConnectivitySlot.MOBILE) }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val alwaysUseRsrpLevelForLte: StateFlow<Boolean> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { it.showRsrpSignalLevelforLTE }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val hideNoInternetState: StateFlow<Boolean> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { it.hideNoInternetState }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val networkTypeIconCustomization: StateFlow<MobileIconCustomizationMode> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { defaultConfig ->
                val enabled = defaultConfig.alwaysShowNetworkTypeIcon
                    || defaultConfig.enableDdsRatIconEnhancement
                    || defaultConfig.enableRatIconEnhancement
                val state = MobileIconCustomizationMode(
                    isRatCustomization = enabled,
                    alwaysShowNetworkTypeIcon = defaultConfig.alwaysShowNetworkTypeIcon,
                    ddsRatIconEnhancementEnabled = defaultConfig.enableDdsRatIconEnhancement,
                    nonDdsRatIconEnhancementEnabled = defaultConfig.enableRatIconEnhancement,
                )
                state
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(), MobileIconCustomizationMode())

    override val showVolteIcon: StateFlow<Boolean> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { it.showVolteIcon }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val showVowifiIcon: StateFlow<Boolean> =
        mobileConnectionsRepo.defaultDataSubRatConfig
            .mapLatest { it.showVowifiIcon }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val isRoamingForceHidden: Flow<Boolean> =
        connectivityRepository.forceHiddenSlots
            .map { it.contains(ConnectivitySlot.ROAMING) }
            .stateIn(scope, SharingStarted.WhileSubscribed(), false)

    override val isMobileHdForceHidden: Flow<Boolean> =
        commonImsRepo.imsIconState
            .map { !it.showHdIcon }
            .stateIn(scope, SharingStarted.WhileSubscribed(), true)

    override val isVoWifiForceHidden: Flow<Boolean> =
        commonImsRepo.imsIconState
            .map { !it.showVowifiIcon }
            .stateIn(scope, SharingStarted.WhileSubscribed(), true)

    /** Vends out new [MobileIconInteractor] for a particular subId */
    override fun getMobileConnectionInteractorForSubId(subId: Int): MobileIconInteractor =
        reuseCache[subId]?.get() ?: createMobileConnectionInteractorForSubId(subId)

    private fun createMobileConnectionInteractorForSubId(subId: Int): MobileIconInteractor =
        MobileIconInteractorImpl(
                scope,
                activeDataConnectionHasDataEnabled,
                alwaysShowDataRatIcon,
                alwaysUseCdmaLevel,
                isSingleCarrier,
                mobileIsDefault,
                defaultMobileIconMapping,
                defaultMobileIconGroup,
                isDefaultConnectionFailed,
                isForceHidden,
                isRoamingForceHidden,
                isMobileHdForceHidden,
                isVoWifiForceHidden,
                mobileConnectionsRepo.getRepoForSubId(subId),
                alwaysUseRsrpLevelForLte,
                hideNoInternetState,
                networkTypeIconCustomization,
                showVolteIcon,
                showVowifiIcon,
                context,
                mobileConnectionsRepo.defaultDataSubId,
            )
            .also { reuseCache[subId] = WeakReference(it) }

    companion object {
        private const val LOGGING_PREFIX = "Intr"
    }
}
