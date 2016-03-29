/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.MathUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile.AnimationIcon;
import com.android.systemui.qs.QSTile.State;

import java.util.Objects;

/** View that represents a standard quick settings tile. **/
public class QSTileView extends ViewGroup {
    private static final Typeface CONDENSED = Typeface.create("sans-serif-condensed",
            Typeface.NORMAL);

    protected final Context mContext;
    private final View mIcon;
    private final View mDivider;
    private final H mHandler = new H();
    private final int mIconSizePx;
    private final int mTileSpacingPx;
    private int mTilePaddingTopPx;
    private final int mTilePaddingBelowIconPx;
    private final int mDualTileVerticalPaddingPx;
    private final View mTopBackgroundView;
    private final Drawable mTileBackground;
    private final Drawable mTileTopBackground;

    private TextView mLabel;
    private QSDualTileLabel mDualLabel;
    private boolean mDual;
    private OnClickListener mClickPrimary;
    private OnClickListener mClickSecondary;
    private OnLongClickListener mLongClick;
    private RippleDrawable mRipple;
    private RippleDrawable mTopRipple;
    private float mRealElevation;

    public QSTileView(Context context) {
        super(context);

        mContext = context;
        final Resources res = context.getResources();
        mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        mTileSpacingPx = res.getDimensionPixelSize(R.dimen.qs_tile_spacing);
        mTilePaddingBelowIconPx =  res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        mDualTileVerticalPaddingPx =
                res.getDimensionPixelSize(R.dimen.qs_dual_tile_padding_vertical);
        mTileBackground = newTileBackground();
        mTileTopBackground = newTileBackground();
        recreateLabel();
        setClipChildren(false);

        mTopBackgroundView = new View(context);
        mTopBackgroundView.setId(View.generateViewId());
        addView(mTopBackgroundView);

        mIcon = createIcon();
        addView(mIcon);

        mDivider = new View(mContext);
        mDivider.setBackgroundColor(context.getColor(R.color.qs_tile_divider));
        final int dh = res.getDimensionPixelSize(R.dimen.qs_tile_divider_height);
        mDivider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dh));
        addView(mDivider);

        setClickable(true);
        updateTopPadding();
        setId(View.generateViewId());
    }

    private void updateTopPadding() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top);
        int largePadding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale,
                1.0f, FontSizeUtils.LARGE_TEXT_SCALE) - 1f) / (FontSizeUtils.LARGE_TEXT_SCALE - 1f);
        mTilePaddingTopPx = Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
        requestLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTopPadding();
        FontSizeUtils.updateFontSize(mLabel, R.dimen.qs_tile_text_size);
        if (mDualLabel != null) {
            mDualLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.qs_tile_text_size));
        }
    }

    private synchronized void recreateLabel() {
        final CharSequence labelText;
        final CharSequence labelDescription;
        if (mDualLabel != null) {
            labelText = mDualLabel.getText();
            labelDescription = mDualLabel.getContentDescription();
        } else if (mLabel != null) {
            labelText = mLabel.getText();
            labelDescription = mLabel.getContentDescription();
        } else {
            labelText = "";
            labelDescription = "";
        }

        final Resources res = mContext.getResources();
        if (mDual) {
            if (mLabel != null) {
                removeView(mLabel);
                mLabel = null;
            }

            final QSDualTileLabel dualLabel = mDualLabel == null ?
                    new QSDualTileLabel(mContext) : mDualLabel;
            dualLabel.setId(View.generateViewId());
            dualLabel.setBackgroundResource(R.drawable.btn_borderless_rect);
            dualLabel.setFirstLineCaret(mContext.getDrawable(R.drawable.qs_dual_tile_caret));
            dualLabel.setTextColor(mContext.getColor(R.color.qs_tile_text));
            dualLabel.setPadding(0, mDualTileVerticalPaddingPx, 0, mDualTileVerticalPaddingPx);
            dualLabel.setTypeface(CONDENSED);
            dualLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
            dualLabel.setOnClickListener(mClickSecondary);
            dualLabel.setClickable(mClickSecondary != null);
            dualLabel.setOnLongClickListener(mLongClick);
            dualLabel.setLongClickable(mLongClick != null);
            dualLabel.setFocusable(true);
            dualLabel.setText(labelText);
            dualLabel.setContentDescription(labelDescription);
            if (mDualLabel == null) {
                addView(mDualLabel = dualLabel);
            }
        } else {
            if (mDualLabel != null) {
                mDualLabel.setOnClickListener(null);
                mDualLabel.setClickable(false);
                mDualLabel.setOnLongClickListener(null);
                mDualLabel.setLongClickable(false);
                mDualLabel.setFocusable(false);

                removeView(mDualLabel);
                mDualLabel = null;
            }

            final TextView label = mLabel == null ? new TextView(mContext) : mLabel;
            label.setId(android.R.id.title);
            label.setTextColor(mContext.getColor(R.color.qs_tile_text));
            label.setGravity(Gravity.CENTER_HORIZONTAL);
            label.setMinLines(2);
            label.setPadding(0, 0, 0, 0);
            label.setTypeface(CONDENSED);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    res.getDimensionPixelSize(R.dimen.qs_tile_text_size));
            label.setClickable(false);
            label.setText(labelText);
            label.setContentDescription(labelDescription);
            if (mLabel == null) {
                addView(mLabel = label);
            }
        }
    }

    public boolean isDual() {
        return mDual;
    }

    public synchronized void setDual(final boolean dual) {
        final boolean changed = dual != mDual;
        mDual = dual;

        mRipple = (mTileBackground instanceof RippleDrawable)
                ? ((RippleDrawable) mTileBackground) : null;
        mTopRipple = (mTileTopBackground instanceof RippleDrawable)
                ? ((RippleDrawable) mTileTopBackground) : null;
        if (getWidth() != 0) {
            updateRippleSize(getWidth(), getHeight());
        }

        final View priority = dual ? mTopBackgroundView : this;
        final View other = dual ? this : mTopBackgroundView;

        priority.setOnClickListener(mClickPrimary);
        priority.setClickable(mClickPrimary != null);
        priority.setOnLongClickListener(mLongClick);
        priority.setLongClickable(mLongClick != null);
        other.setOnClickListener(null);
        other.setClickable(false);
        other.setOnLongClickListener(null);
        other.setLongClickable(false);

        setImportantForAccessibility(dual ? View.IMPORTANT_FOR_ACCESSIBILITY_NO :
                View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        mTopBackgroundView.setBackground(dual ? mTileTopBackground : null);
        setBackground(dual ? null : mTileBackground);

        mTopBackgroundView.setFocusable(dual);
        setFocusable(!dual);
        mDivider.setVisibility(dual ? VISIBLE : GONE);
        if (changed) {
            recreateLabel();
            updateTopPadding();
        }
        postInvalidate();
    }

    public void init(OnClickListener clickPrimary, OnClickListener clickSecondary,
            OnLongClickListener longClick) {
        mClickPrimary = clickPrimary;
        mClickSecondary = clickSecondary;
        mLongClick = longClick;

        final View priority = mDual ? mTopBackgroundView : this;
        final View other = mDual ? this : mTopBackgroundView;

        if (priority != null) {
            priority.setOnClickListener(clickPrimary);
            priority.setClickable(clickPrimary != null);

            priority.setOnLongClickListener(longClick);
            priority.setLongClickable(longClick != null);
        }

        if (other != null) {
            other.setOnClickListener(null);
            other.setClickable(false);

            other.setOnLongClickListener(null);
            other.setLongClickable(false);
        }

        if (mDualLabel != null) {
            mDualLabel.setOnClickListener(clickSecondary);
            mDualLabel.setClickable(clickSecondary != null);

            mDualLabel.setOnLongClickListener(longClick);
            mDualLabel.setLongClickable(longClick != null);
        }
    }

    protected View createIcon() {
        final ImageView icon = new ImageView(mContext);
        icon.setId(android.R.id.icon);
        icon.setScaleType(ScaleType.CENTER_INSIDE);
        return icon;
    }

    private Drawable newTileBackground() {
        final int[] attrs = new int[] { android.R.attr.selectableItemBackgroundBorderless };
        final TypedArray ta = mContext.obtainStyledAttributes(attrs);
        final Drawable d = ta.getDrawable(0);
        ta.recycle();
        return d;
    }

    private View labelView() {
        return mDual ? mDualLabel : mLabel;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int w = MeasureSpec.getSize(widthMeasureSpec);
        final int h = MeasureSpec.getSize(heightMeasureSpec);
        final int iconSpec = exactly(mIconSizePx);
        mIcon.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST), iconSpec);
        labelView().measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST));
        if (mDual) {
            mDivider.measure(widthMeasureSpec, exactly(mDivider.getLayoutParams().height));
        }
        int heightSpec = exactly(
                mIconSizePx + mTilePaddingBelowIconPx + mTilePaddingTopPx);
        mTopBackgroundView.measure(widthMeasureSpec, heightSpec);
        setMeasuredDimension(w, h);
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getMeasuredWidth();
        final int h = getMeasuredHeight();

        layout(mTopBackgroundView, 0, mTileSpacingPx);

        int top = 0;
        top += mTileSpacingPx;
        top += mTilePaddingTopPx;
        final int iconLeft = (w - mIcon.getMeasuredWidth()) / 2;
        layout(mIcon, iconLeft, top);
        updateRippleSize(w, h);
        top = mIcon.getBottom();
        top += mTilePaddingBelowIconPx;
        if (mDual) {
            layout(mDivider, 0, top);
            top = mDivider.getBottom();
        }
        layout(labelView(), 0, top);
    }

    private void updateRippleSize(int width, int height) {
        // center the touch feedback on the center of the icon, and dial it down a bit
        final int cx = width / 2;
        final int cy = mDual ? mIcon.getTop() + mIcon.getHeight() / 2 : height / 2;
        final int rad = (int)(mIcon.getHeight() * 1.25f);
        if (mRipple != null) {
            mRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
        }
        if (mTopRipple != null) {
            mTopRipple.setHotspotBounds(cx - rad, cy - rad, cx + rad, cy + rad);
        }
    }

    private static void layout(View child, int left, int top) {
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
    }

    protected void handleStateChanged(QSTile.State state) {
        if (mIcon instanceof ImageView) {
            setIcon((ImageView) mIcon, state);
        }
        if (mDual) {
            mDualLabel.setText(state.label);
            mDualLabel.setContentDescription(state.dualLabelContentDescription);
            mTopBackgroundView.setContentDescription(state.contentDescription);
        } else {
            mLabel.setText(state.label);
            setContentDescription(state.contentDescription);
        }
    }

    protected void setIcon(ImageView iv, QSTile.State state) {
        if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            Drawable d = state.icon != null ? state.icon.getDrawable(mContext) : null;
            if (d != null && state.autoMirrorDrawable) {
                d.setAutoMirrored(true);
            }
            iv.setImageDrawable(d);
            iv.setTag(R.id.qs_icon_tag, state.icon);
            if (d instanceof Animatable) {
                Animatable a = (Animatable) d;
                if (state.icon instanceof AnimationIcon && !iv.isShown()) {
                    a.stop(); // skip directly to end state
                }
            }
        }
    }

    public void onStateChanged(QSTile.State state) {
        mHandler.obtainMessage(H.STATE_CHANGED, state).sendToTarget();
    }

    /**
     * Update the accessibility order for this view.
     *
     * @param previousView the view which should be before this one
     * @return the last view in this view which is accessible
     */
    public View updateAccessibilityOrder(View previousView) {
        View firstView;
        View lastView;
        if (mDual) {
            lastView = mDualLabel;
            firstView = mTopBackgroundView;
        } else {
            firstView = this;
            lastView = this;
        }
        firstView.setAccessibilityTraversalAfter(previousView.getId());
        return lastView;
    }

    public View.DragShadowBuilder getDragShadowBuilder() {
        return new View.DragShadowBuilder(mIcon);
    }

    private class H extends Handler {
        private static final int STATE_CHANGED = 1;
        public H() {
            super(Looper.getMainLooper());
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STATE_CHANGED) {
                handleStateChanged((State) msg.obj);
            }
        }
    }
}
