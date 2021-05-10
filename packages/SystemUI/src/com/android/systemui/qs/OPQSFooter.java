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
 * limitations under the License
 */

package com.android.systemui.qs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SettingsButton;

public class OPQSFooter extends LinearLayout {

    private SettingsButton mSettingsButton;
    protected View mEdit;
    protected View mBrightnessIcon;
    protected View mBrightnessMirror;
    protected TouchAnimator mFooterAnimator;
    protected TouchAnimator mBrightnessAnimator;
    private Boolean mExpanded;
    private Boolean mIsLandscape;
    private FrameLayout mFooterActions;

    public OPQSFooter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mEdit = findViewById(R.id.edit);
        mSettingsButton = findViewById(R.id.settings_button);
        mFooterActions = findViewById(R.id.op_qs_footer_actions);
        mFooterAnimator = createFooterAnimator();
    }

    void updateAnimators() {
        final float iconWidth = mBrightnessIcon.getWidth();
        if (mBrightnessMirror != null && iconWidth != mIconWidth) {
            mBrightnessAnimator = createBrightnessAnimator();
        }
    }

    void setExpansion(float headerExpansionFraction) {
        if (mFooterAnimator != null) {
            mFooterAnimator.setPosition(headerExpansionFraction);
        }
        if (mBrightnessAnimator != null) {
            mBrightnessAnimator.setPosition(headerExpansionFraction);
        }
    }

    void setMirror(View mirror) {
        mBrightnessMirror = mirror;
        updateAnimators();
    }

    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
        if (mEdit != null) {
            int visibility = mExpanded ? View.VISIBLE : View.GONE;
            mEdit.setVisibility(visibility);
        }
        if (mBrightnessIcon != null) {
            mBrightnessIcon.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Nullable
    private TouchAnimator createFooterAnimator() {
        return new TouchAnimator.Builder()
                .addFloat(mEdit, "alpha", 0, 0, 1)
                .setStartDelay(0.9f)
                .build();
    }

    @Nullable
    private TouchAnimator createBrightnessAnimator() {
        mIconWidth = mBrightnessIcon.getWidth();
        MarginLayoutParams lp = (MarginLayoutParams) mBrightnessIcon.getLayoutParams();
        final float translation = mIconWidth + lp.getMarginStart();
        return new TouchAnimator.Builder()
                .addFloat(findViewById(R.id.brightness_slider), "translationX", isLayoutRtl()
                        ? -translation/2 : translation/2, 0)
                .addFloat(mBrightnessMirror.findViewById(R.id.brightness_slider), "translationX",
                        isLayoutRtl() ? -translation/2 : translation/2, 0)
                .addFloat(mBrightnessIcon, "translationX", isLayoutRtl() ? -translation : translation, 0)
                .addFloat(mBrightnessIcon, "rotation", 120, 0)
                .addFloat(mBrightnessIcon, "alpha", 0, 1)
                .build();
    }

    View getSettingsButton() {
        return mSettingsButton;
    }

    View getEditButton() {
        return mEdit;
    }
}
