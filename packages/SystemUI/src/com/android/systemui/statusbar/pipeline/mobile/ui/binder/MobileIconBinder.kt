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

package com.android.systemui.statusbar.pipeline.mobile.ui.binder

import android.content.res.ColorStateList
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Space
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.settingslib.graph.SignalDrawable
import com.android.systemui.R
import com.android.systemui.common.ui.binder.ContentDescriptionViewBinder
import com.android.systemui.common.ui.binder.IconViewBinder
import com.android.systemui.lifecycle.repeatWhenAttached
import com.android.systemui.statusbar.StatusBarIconView
import com.android.systemui.statusbar.StatusBarIconView.STATE_HIDDEN
import com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger
import com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel
import com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewBinding
import com.android.systemui.statusbar.pipeline.shared.ui.binder.ModernStatusBarViewVisibilityHelper
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object MobileIconBinder {
    /** Binds the view to the view-model, continuing to update the former based on the latter */
    @JvmStatic
    fun bind(
        view: ViewGroup,
        viewModel: LocationBasedMobileViewModel,
        @StatusBarIconView.VisibleState initialVisibilityState: Int = STATE_HIDDEN,
        logger: MobileViewLogger,
    ): ModernStatusBarViewBinding {
        val mobileGroupView = view.requireViewById<ViewGroup>(R.id.mobile_group)
        val activityContainer = view.requireViewById<View>(R.id.inout_container)
        val activityIn = view.requireViewById<ImageView>(R.id.mobile_in)
        val activityOut = view.requireViewById<ImageView>(R.id.mobile_out)
        val networkTypeView = view.requireViewById<ImageView>(R.id.mobile_type)
        val iconView = view.requireViewById<ImageView>(R.id.mobile_signal)
        val mobileDrawable = SignalDrawable(view.context).also { iconView.setImageDrawable(it) }
        val mobileHdView = view.requireViewById<ImageView>(R.id.mobile_hd)
        val mobileHdSpace = view.requireViewById<Space>(R.id.mobile_hd_space)
        val dotView = view.requireViewById<StatusBarIconView>(R.id.status_bar_dot)
        val volteView = view.requireViewById<ImageView>(R.id.mobile_volte)

        view.isVisible = viewModel.isVisible.value
        iconView.isVisible = true

        // TODO(b/238425913): We should log this visibility state.
        @StatusBarIconView.VisibleState
        val visibilityState: MutableStateFlow<Int> = MutableStateFlow(initialVisibilityState)

        val iconTint: MutableStateFlow<Int> = MutableStateFlow(viewModel.defaultColor)
        val decorTint: MutableStateFlow<Int> = MutableStateFlow(viewModel.defaultColor)

        var isCollecting = false

        view.repeatWhenAttached {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.CREATED) {
                    // isVisible controls the visibility state of the outer group, and thus it needs
                    // to run in the CREATED lifecycle so it can continue to watch while invisible
                    // See (b/291031862) for details
                    launch {
                        viewModel.isVisible.collect { isVisible ->
                            viewModel.verboseLogger?.logBinderReceivedVisibility(
                                view,
                                viewModel.subscriptionId,
                                isVisible
                            )
                            view.isVisible = isVisible
                            // [StatusIconContainer] can get out of sync sometimes. Make sure to
                            // request another layout when this changes.
                            view.requestLayout()
                        }
                    }
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    logger.logCollectionStarted(view, viewModel)
                    isCollecting = true

                    launch {
                        visibilityState.collect { state ->
                            ModernStatusBarViewVisibilityHelper.setVisibilityState(
                                state,
                                mobileGroupView,
                                dotView,
                            )
                        }
                    }

                    // Set the icon for the triangle
                    launch {
                        viewModel.icon.distinctUntilChanged().collect { icon ->
                            viewModel.verboseLogger?.logBinderReceivedSignalIcon(
                                view,
                                viewModel.subscriptionId,
                                icon,
                            )
                            mobileDrawable.level = icon.toSignalDrawableState()
                        }
                    }

                    launch {
                        viewModel.contentDescription.distinctUntilChanged().collect {
                            ContentDescriptionViewBinder.bind(it, view)
                        }
                     }

                    // Set the network type icon
                    launch {
                        viewModel.networkTypeIcon.distinctUntilChanged().collect { dataTypeId ->
                            viewModel.verboseLogger?.logBinderReceivedNetworkTypeIcon(
                                view,
                                viewModel.subscriptionId,
                                dataTypeId,
                            )
                            dataTypeId?.let { IconViewBinder.bind(dataTypeId, networkTypeView) }
                            networkTypeView.visibility = if (dataTypeId != null) VISIBLE else GONE
                        }
                    }

                    // Set the mobile HD indicator (VoLTE/VoNR)
                    launch {
                        viewModel.showHd.distinctUntilChanged().collect { isHd ->
                            mobileHdView.isVisible = isHd
                            mobileHdSpace.isVisible = isHd
                        }
                     }

                    // Set the activity indicators
                    launch { viewModel.activityInVisible.collect { activityIn.isVisible = it } }

                    launch { viewModel.activityOutVisible.collect { activityOut.isVisible = it } }

                    launch {
                        viewModel.activityContainerVisible.collect {
                            activityContainer.isVisible = it
                        }
                    }

                    // Set the tint
                    launch {
                        iconTint.collect { tint ->
                            val tintList = ColorStateList.valueOf(tint)
                            iconView.imageTintList = tintList
                            networkTypeView.imageTintList = tintList
                            mobileHdView.imageTintList = tintList
                            activityIn.imageTintList = tintList
                            activityOut.imageTintList = tintList
                            dotView.setDecorColor(tint)
                            volteView.imageTintList = tintList
                        }
                     }

                    launch { decorTint.collect { tint -> dotView.setDecorColor(tint) } }

                launch {
                    viewModel.volteId.distinctUntilChanged().collect { volteId ->
                        if (volteId != 0) {
                            volteView.visibility = VISIBLE
                            volteView.setImageResource(volteId)
                        } else {
                            volteView.visibility = GONE
                        }
                    }
                }

                launch { viewModel.showSignalStrengthIcon.collect { iconView.isVisible = it } }

                    try {
                        awaitCancellation()
                    } finally {
                        isCollecting = false
                        logger.logCollectionStopped(view, viewModel)
                    }
                }
            }
        }

        return object : ModernStatusBarViewBinding {
            override fun getShouldIconBeVisible(): Boolean {
                return viewModel.isVisible.value
            }

            override fun onVisibilityStateChanged(@StatusBarIconView.VisibleState state: Int) {
                visibilityState.value = state
            }

            override fun onIconTintChanged(newTint: Int) {
                iconTint.value = newTint
            }

            override fun onDecorTintChanged(newTint: Int) {
                decorTint.value = newTint
            }

            override fun isCollecting(): Boolean {
                return isCollecting
            }
        }
    }
}
