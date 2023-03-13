/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.systemui.statusbar.pipeline.mobile.data.model

data class NetworkTypeIconCustomizationMode(
    val enabled: Boolean = false,
    val alwaysShowNetworkTypeIcon: Boolean = false,
    val ddsRatIconEnhancementEnabled: Boolean = false,
    val nonDdsRatIconEnhancementEnabled: Boolean = false,
    val mobileDataEnabled: Boolean = false,
    val dataRoamingEnabled: Boolean = false,
    val isDefaultDataSub: Boolean = false,
    val isRoaming: Boolean = false,
)