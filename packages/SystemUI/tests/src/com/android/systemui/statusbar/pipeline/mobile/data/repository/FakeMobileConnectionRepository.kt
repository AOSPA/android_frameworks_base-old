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

package com.android.systemui.statusbar.pipeline.mobile.data.repository

import com.android.systemui.log.table.TableLogBuffer
import com.android.systemui.statusbar.pipeline.mobile.data.model.MobileConnectionModel
import com.android.systemui.statusbar.pipeline.mobile.data.model.NetworkNameModel
import com.android.systemui.statusbar.pipeline.mobile.data.repository.MobileConnectionRepository.Companion.DEFAULT_NUM_LEVELS
import kotlinx.coroutines.flow.MutableStateFlow

// TODO(b/261632894): remove this in favor of the real impl or DemoMobileConnectionRepository
class FakeMobileConnectionRepository(
    override val subId: Int,
    override val tableLogBuffer: TableLogBuffer,
) : MobileConnectionRepository {
    private val _connectionInfo = MutableStateFlow(MobileConnectionModel())
    override val connectionInfo = _connectionInfo

    override val numberOfLevels = MutableStateFlow(DEFAULT_NUM_LEVELS)

    private val _dataEnabled = MutableStateFlow(true)
    override val dataEnabled = _dataEnabled

    override val cdmaRoaming = MutableStateFlow(false)

    override val networkName =
        MutableStateFlow<NetworkNameModel>(NetworkNameModel.Default("default"))

    private val _dataRoamingEnabled = MutableStateFlow(true)
    override val dataRoamingEnabled = _dataRoamingEnabled

    fun setConnectionInfo(model: MobileConnectionModel) {
        _connectionInfo.value = model
    }

    fun setDataEnabled(enabled: Boolean) {
        _dataEnabled.value = enabled
    }

    override val imsInfo = MutableStateFlow(MobileConnectionModel())

    fun setDataRoamingEnabled(enabled: Boolean) {
        _dataRoamingEnabled.value= enabled
    }
}
