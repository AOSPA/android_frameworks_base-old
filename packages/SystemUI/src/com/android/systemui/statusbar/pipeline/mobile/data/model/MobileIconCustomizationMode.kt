/*
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.systemui.statusbar.pipeline.mobile.data.model

import android.telephony.CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN
import android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN

data class MobileIconCustomizationMode(
    val alwaysUseRsrpLevelForLte: Boolean = false,
    val lteRsrpLevel: Int = SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
    val dataNetworkType: Int = NETWORK_TYPE_UNKNOWN,
    val voiceNetworkType: Int = NETWORK_TYPE_UNKNOWN,
)