/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.systemui.statusbar.phone;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.android.keyguard.AlphaOptimizedImageButton;

public class QsAddButton extends AlphaOptimizedImageButton {
    private ObjectAnimator mAnimator = null;

    public QsAddButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public void rotate(final float deg) {
        if (mAnimator != null) {
            mAnimator.removeAllListeners();
            mAnimator.cancel();
        }

        mAnimator = ObjectAnimator.ofFloat(this, View.ROTATION, getRotation(), deg);
        mAnimator.setDuration(350);
        mAnimator.setInterpolator(AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.linear));
        mAnimator.start();
    }
}
