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

/**
 * Changes from Qualcomm Innovation Center are provided under the following license:
 * Copyright (c) 2023 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.systemui.statusbar.pipeline.mobile.ui

import android.telephony.TelephonyManager
import com.android.systemui.CoreStartable
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.statusbar.phone.StatusBarIconController
import com.android.systemui.statusbar.pipeline.StatusBarPipelineFlags
import com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MobileIconsInteractor
import com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MobileIconsViewModel
import java.io.PrintWriter
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * This class is intended to provide a context to collect on the
 * [MobileIconsInteractor.filteredSubscriptions] data source and supply a state flow that can
 * control [StatusBarIconController] to keep the old UI in sync with the new data source.
 *
 * It also provides a mechanism to create a top-level view model for each IconManager to know about
 * the list of available mobile lines of service for which we want to show icons.
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@SysUISingleton
class MobileUiAdapter
@Inject
constructor(
    private val iconController: StatusBarIconController,
    val mobileIconsViewModel: MobileIconsViewModel,
    private val logger: MobileViewLogger,
    @Application private val scope: CoroutineScope,
    private val statusBarPipelineFlags: StatusBarPipelineFlags,
    private val telephonyManager: TelephonyManager,
) : CoreStartable {
    private var isCollecting: Boolean = false
    private var lastValue: List<Int>? = null
    private var isMultiSimEnabled: Boolean = telephonyManager.isMultiSimEnabled()

    override fun start() {
        // Only notify the icon controller if we want to *render* the new icons.
        // Note that this flow may still run if
        // [statusBarPipelineFlags.runNewMobileIconsBackend] is true because we may want to
        // get the logging data without rendering.
        if (statusBarPipelineFlags.useNewMobileIcons()) {
            scope.launch {
                isCollecting = true
                mobileIconsViewModel.subscriptionIdsFlow.collectLatest {
                    logger.logUiAdapterSubIdsSentToIconController(it)
                    if (isMultiSimConfigChanged()) {
                        isMultiSimEnabled = telephonyManager.isMultiSimEnabled()
                        if (lastValue != null && lastValue!!.size != it.size) {
                            iconController.setNewMobileIconSubIds(emptyList())
                        }
                    }
                    lastValue = it
                    iconController.setNewMobileIconSubIds(it)
                }
            }
        }
    }

    override fun dump(pw: PrintWriter, args: Array<out String>) {
        pw.println("isCollecting=$isCollecting")
        pw.println("Last values sent to icon controller: $lastValue")
    }

    private fun isMultiSimConfigChanged(): Boolean {
        return isMultiSimEnabled != telephonyManager.isMultiSimEnabled()
    }
}
