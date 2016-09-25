/*
 * Copyright 2014-2016 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.pie;

import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Pie menu item
 * View holder for a pie slice.
 */
public class PieItem {

    private float animate;
    private float start;

    private boolean mSelected;
    private boolean mIsLesser;

    private List<PieItem> mItems;

    private String mName;

    private View mView;

    private int mSize;

    /**
     * Creates a new pie item
     *
     * @Param view the item view
     * @Param conext the current context
     * @Param name the name used to refrence the item
     * @Param lesser the pie level on pie T/F = 1/2
     * @Param size the item size
     */
    public PieItem(View view, String name, boolean lesser, int size) {
        mView = view;
        setAnimationAngle(getAnimationAngle());
        setName(name);
        mIsLesser = lesser;
        mSize = size;
    }

    protected boolean isLesser() {
        return mIsLesser;
    }

    protected String getName() {
        return mName;
    }

    private void setName(String name) {
        mName = name;
        mView.setTag(mName);
    }

    protected void addItem(PieItem item) {
        if (mItems == null) {
            mItems = new ArrayList<PieItem>();
        }
        mItems.add(item);
    }

    private float getAnimationAngle() {
        return animate;
    }

    protected void setSelected(boolean s) {
        mSelected = s;
        if (mView != null) {
            mView.setSelected(s);
        }
    }

    protected boolean isSelected() {
        return mSelected;
    }

    private void setAnimationAngle(float a) {
        animate = a;
    }

    protected void setGeometry(float st) {
        start = st;
    }

    protected float getStartAngle() {
        return start + animate;
    }

    protected View getView() {
        return mView;
    }

    protected void setIcon(int resId) {
        ((ImageView) mView).setImageResource(resId);
    }

    protected int getSize() {
        return mSize;
    }
}
