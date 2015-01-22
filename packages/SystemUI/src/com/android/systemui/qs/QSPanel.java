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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile.DetailAdapter;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/** View that represents the quick settings tile panel. **/
public class QSPanel extends ViewGroup {
    private static final float TILE_ASPECT = 1.2f;

    private final Context mContext;
    private final ArrayList<TileRecord> mRecords = new ArrayList<TileRecord>();
    private final View mDetail;
    private final ViewGroup mDetailContent;
    private final TextView mDetailSettingsButton;
    private final TextView mDetailDoneButton;
    private final View mBrightnessView;
    private final QSDetailClipper mClipper;
    private final CurrentUserTracker mUserTracker;
    private final H mHandler = new H();

    private int mColumns;
    private int mCellWidth;
    private int mCellHeight;
    private int mLargeCellWidth;
    private int mLargeCellHeight;
    private int mPanelPaddingBottom;
    private int mDualTileUnderlap;
    private int mBrightnessPaddingTop;
    private int mGridHeight;
    private int mRowCount = 0;
    private int mDualCount = 0;
    private boolean mExpanded;
    private boolean mListening;
    private boolean mClosingDetail;

    private Record mDetailRecord;
    private Callback mCallback;
    private BrightnessController mBrightnessController;
    private QSTileHost mHost;

    private QSFooter mFooter;
    private boolean mGridContentVisible = true;

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mDetail = LayoutInflater.from(context).inflate(R.layout.qs_detail, this, false);
        mDetailContent = (ViewGroup) mDetail.findViewById(android.R.id.content);
        mDetailSettingsButton = (TextView) mDetail.findViewById(android.R.id.button2);
        mDetailDoneButton = (TextView) mDetail.findViewById(android.R.id.button1);
        updateDetailText();
        mDetail.setVisibility(GONE);
        mDetail.setClickable(true);
        mBrightnessView = LayoutInflater.from(context).inflate(
                R.layout.quick_settings_brightness_dialog, this, false);
        mFooter = new QSFooter(this, context);
        addView(mDetail);
        addView(mBrightnessView);
        addView(mFooter.getView());
        mClipper = new QSDetailClipper(mDetail);
        updateResources();

        mBrightnessController = new BrightnessController(getContext(),
                (ImageView) findViewById(R.id.brightness_icon),
                (ToggleSlider) findViewById(R.id.brightness_slider));

        mDetailDoneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                announceForAccessibility(
                        mContext.getString(R.string.accessibility_desc_quick_settings));
                closeDetail();
            }
        });

        (mUserTracker = new CurrentUserTracker(mContext) {

            @Override
            public void onUserSwitched(int newUserId) {
                // no-op; we just use this object for the current uid
            }

        }).startTracking();
    }

    private void updateDetailText() {
        mDetailDoneButton.setText(R.string.quick_settings_done);
        mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
    }

    public void setBrightnessMirror(BrightnessMirrorController c) {
        super.onFinishInflate();
        ToggleSlider brightnessSlider = (ToggleSlider) findViewById(R.id.brightness_slider);
        ToggleSlider mirror = (ToggleSlider) c.getMirror().findViewById(R.id.brightness_slider);
        brightnessSlider.setMirror(mirror);
        brightnessSlider.setMirrorController(c);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public synchronized void setHost(QSTileHost host) {
        if (mHost != null) {
            // we don't want any updates from the old host
            host.setCallback(null);
        }

        mHost = host;
        mFooter.setHost(host);

        host.setCallback(new QSTileHost.Callback() {
            @Override
            public void onTilesChanged() {
                setTiles();
            }
        });
        setTiles();
    }

    public QSTileHost getHost() {
        return mHost;
    }

    public void updateResources() {
        final Resources res = mContext.getResources();
        final int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        mCellHeight = res.getDimensionPixelSize(R.dimen.qs_tile_height);
        mCellWidth = (int)(mCellHeight * TILE_ASPECT);
        mLargeCellHeight = res.getDimensionPixelSize(R.dimen.qs_dual_tile_height);
        mLargeCellWidth = (int)(mLargeCellHeight * TILE_ASPECT);
        mPanelPaddingBottom = res.getDimensionPixelSize(R.dimen.qs_panel_padding_bottom);
        mDualTileUnderlap = res.getDimensionPixelSize(R.dimen.qs_dual_tile_padding_vertical);
        mBrightnessPaddingTop = res.getDimensionPixelSize(R.dimen.qs_brightness_padding_top);
        if (mColumns != columns) {
            mColumns = columns;
            postInvalidate();
        }
        for (TileRecord r : mRecords) {
            r.tile.clearState();
        }
        if (mListening) {
            refreshAllTiles();
        }
        updateDetailText();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(mDetailDoneButton, R.dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(mDetailSettingsButton, R.dimen.qs_detail_button_text_size);

        // We need to poke the detail views as well as they might not be attached to the view
        // hierarchy but reused at a later point.
        synchronized (mRecords) {
            int count = mRecords.size();
            for (int i = 0; i < count; i++) {
                View detailView = mRecords.get(i).detailView;
                if (detailView != null) {
                    detailView.dispatchConfigurationChanged(newConfig);
                }
            }
        }
        mFooter.onConfigurationChanged();
    }

    public void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        MetricsLogger.visibility(mContext, MetricsLogger.QS_PANEL, mExpanded);
        if (!mExpanded) {
            closeDetail();
        } else {
            logTiles();
        }
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        synchronized (mRecords) {
            for (TileRecord r : mRecords) {
                r.tile.setListening(mListening);
            }
        }
        mFooter.setListening(mListening);
        if (mListening) {
            refreshAllTiles();
        }
        if (listening) {
            mBrightnessController.registerCallbacks();
        } else {
            mBrightnessController.unregisterCallbacks();
        }
    }

    public void refreshAllTiles() {
        synchronized (mRecords) {
            for (TileRecord r : mRecords) {
                r.tile.refreshState();
            }
        }
        mFooter.refreshState();
    }

    public void showDetailAdapter(boolean show, DetailAdapter adapter, int[] locationInWindow) {
        int xInWindow = locationInWindow[0];
        int yInWindow = locationInWindow[1];
        mDetail.getLocationInWindow(locationInWindow);

        Record r = new Record();
        r.detailAdapter = adapter;
        r.x = xInWindow - locationInWindow[0];
        r.y = yInWindow - locationInWindow[1];

        locationInWindow[0] = xInWindow;
        locationInWindow[1] = yInWindow;

        showDetail(show, r);
    }

    private void showDetail(boolean show, Record r) {
        mHandler.obtainMessage(H.SHOW_DETAIL, show ? 1 : 0, 0, r).sendToTarget();
    }

    private void setTileVisibility(View v, int visibility) {
        mHandler.obtainMessage(H.SET_TILE_VISIBILITY, visibility, 0, v).sendToTarget();
    }

    private void handleSetTileVisibility(View v, int visibility) {
        if (visibility == VISIBLE && !mGridContentVisible) {
            visibility = INVISIBLE;
        }
        if (visibility == v.getVisibility()) return;
        v.setVisibility(visibility);
    }

    public void setTiles() {
        synchronized (mRecords) {
            final TileRecord[] oldTileRecords = mRecords.toArray(new TileRecord[mRecords.size()]);
            final QSTile<?>[] newTiles = mHost.getTiles();

            if (oldTileRecords.length == newTiles.length) {
                boolean allMatch = true;
                for (int i = 0; i < oldTileRecords.length; i++) {
                    if (!oldTileRecords[i].tile.equals(newTiles[i])) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    // the resulting arrays match up exactly; just return
                    return;
                }
            }

            for (TileRecord record : mRecords) {
                removeView(record.tileView);
            }
            mRecords.clear();

            for (QSTile<?> tile : newTiles) {
                addTile(tile);
            }
        }

        if (isShowingDetail()) {
            mDetail.bringToFront();
        }
    }

    private void drawTile(TileRecord r, QSTile.State state) {
        final int visibility = state.visible ? VISIBLE : GONE;
        setTileVisibility(r.tileView, visibility);
        r.tileView.onStateChanged(state);
    }

    private void addTile(final QSTile<?> tile) {
        final TileRecord r = new TileRecord();
        r.tile = tile;
        r.tileView = tile.createTileView(mContext);
        r.anim = new TileAnimator(r);
        r.tileView.setVisibility(View.GONE);
        final QSTile.Callback callback = new QSTile.Callback() {
            @Override
            public void onStateChanged(QSTile.State state) {
                if (!r.openingDetail) {
                    drawTile(r, state);
                }
            }
            @Override
            public void onShowDetail(boolean show) {
                QSPanel.this.showDetail(show, r);
            }
            @Override
            public void onToggleStateChanged(boolean state) {
                if (mDetailRecord == r) {
                    fireToggleStateChanged(state);
                }
            }
            @Override
            public void onScanStateChanged(boolean state) {
                r.scanState = state;
                if (mDetailRecord == r) {
                    fireScanStateChanged(r.scanState);
                }
            }

            @Override
            public void onAnnouncementRequested(CharSequence announcement) {
                announceForAccessibility(announcement);
            }
        };
        r.tile.setCallback(callback);
        final View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r.tile.click(r.tileView.isDual());
            }
        };
        final View.OnClickListener clickSecondary = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r.tile.secondaryClick();
            }
        };
        final View.OnLongClickListener longClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                r.anim.lift();
                String tileName = r.tile.getClass().getSimpleName();
                r.tileView.startDrag(ClipData.newPlainText("QSTile." + tileName, tileName),
                        r.tileView.getDragShadowBuilder(), r, 0);
                return true;
            }
        };
        final View.OnDragListener drag = new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                final TileRecord ogr = (TileRecord) event.getLocalState();

                switch(event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // no-op
                    return true; // just tell the system we can always accept the drop

                case DragEvent.ACTION_DRAG_ENTERED:
                    if (!r.anim.isAnimating()) {
                        // move the lifted tile if this tile is not moving
                        setTilePosition(ogr, r.row, r.col);
                    }
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:
                    if (!r.anim.isAnimating()) {
                        // move the lifted tile if this tile is not moving
                        setTilePosition(ogr, r.row, r.col);
                    }
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    // no-op
                    return true;

                case DragEvent.ACTION_DROP:
                    // no-op
                    return true; // this value is linked with event.getResult()

                case DragEvent.ACTION_DRAG_ENDED:
                    ogr.anim.drop();
                    return true;
                }

                return false; // in the unlikely case...
            }
        };

        r.tileView.init(click, clickSecondary, longClick, drag);
        r.tile.setListening(mListening);
        callback.onStateChanged(r.tile.getState());
        r.tile.refreshState();
        synchronized (mRecords) {
            mRecords.add(r);
        }

        addView(r.tileView);
    }

    public boolean isShowingDetail() {
        return mDetailRecord != null;
    }

    public void closeDetail() {
        showDetail(false, mDetailRecord);
    }

    public boolean isClosingDetail() {
        return mClosingDetail;
    }

    public int getGridHeight() {
        return mGridHeight;
    }

    private void handleShowDetail(Record r, boolean show) {
        if (r instanceof TileRecord) {
            handleShowDetailTile((TileRecord) r, show);
        } else {
            int x = 0;
            int y = 0;
            if (r != null) {
                x = r.x;
                y = r.y;
            }
            handleShowDetailImpl(r, show, x, y);
        }
    }

    private void handleShowDetailTile(TileRecord r, boolean show) {
        if ((mDetailRecord != null) == show && mDetailRecord == r) return;

        if (show) {
            r.detailAdapter = r.tile.getDetailAdapter();
            if (r.detailAdapter == null) return;
        }
        r.tile.setDetailListening(show);
        int x = r.tileView.getLeft() + r.tileView.getWidth() / 2;
        int y = r.tileView.getTop() + r.tileView.getHeight() / 2;
        handleShowDetailImpl(r, show, x, y);
    }

    private void handleShowDetailImpl(Record r, boolean show, int x, int y) {
        boolean visibleDiff = (mDetailRecord != null) != show;
        if (!visibleDiff && mDetailRecord == r) return;  // already in right state
        DetailAdapter detailAdapter = null;
        AnimatorListener listener = null;
        if (show) {
            detailAdapter = r.detailAdapter;
            r.detailView = detailAdapter.createDetailView(mContext, r.detailView, mDetailContent);
            if (r.detailView == null) throw new IllegalStateException("Must return detail view");

            final Intent settingsIntent = detailAdapter.getSettingsIntent();
            mDetailSettingsButton.setVisibility(settingsIntent != null ? VISIBLE : GONE);
            mDetailSettingsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHost.startActivityDismissingKeyguard(settingsIntent);
                }
            });

            mDetailContent.removeAllViews();
            mDetail.bringToFront();
            mDetailContent.addView(r.detailView);
            MetricsLogger.visible(mContext, detailAdapter.getMetricsCategory());
            announceForAccessibility(mContext.getString(
                    R.string.accessibility_quick_settings_detail,
                    mContext.getString(detailAdapter.getTitle())));
            setDetailRecord(r);
            listener = mHideGridContentWhenDone;
            if (r instanceof TileRecord && visibleDiff) {
                ((TileRecord) r).openingDetail = true;
            }
        } else {
            if (mDetailRecord != null) {
                MetricsLogger.hidden(mContext, mDetailRecord.detailAdapter.getMetricsCategory());
            }
            mClosingDetail = true;
            setGridContentVisibility(true);
            listener = mTeardownDetailWhenDone;
            fireScanStateChanged(false);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        fireShowingDetail(show ? detailAdapter : null);
        if (visibleDiff) {
            mClipper.animateCircularClip(x, y, show, listener);
        }
    }

    private void setGridContentVisibility(boolean visible) {
        mGridContentVisible = visible;
        int newVis = visible ? VISIBLE : INVISIBLE;
        synchronized (mRecords) {
            for (TileRecord tileRecord : mRecords) {
                if (tileRecord.tileView.getVisibility() != GONE) {
                    tileRecord.tileView.setVisibility(newVis);
                }
            }
        }
        mBrightnessView.setVisibility(newVis);
        if (mGridContentVisible != visible) {
            MetricsLogger.visibility(mContext, MetricsLogger.QS_PANEL, newVis);
        }
        mGridContentVisible = visible;
    }

    private void setTilePosition(TileRecord tr, int row, int col) {
        ArrayList<TileRecord> records = new ArrayList<TileRecord>(); // the resulting record list
        String specList = ""; // the resulting tile spec list to be stored

        synchronized (mRecords) {
            // move our special tile out of the way first

            tr.anim.move(row, col);

            // collapse before the special tile

            for (int r = 0; r <= row; r++) {
                for (int c = 0; c < (r == row ? (col + 1) : getPlannedColumnCount(r)); c++) {
                    boolean isFilled = false;
                    for (TileRecord record : mRecords) {
                        if (record.tileView.getVisibility() == VISIBLE &&
                                record.row == r && record.col == c) {
                            isFilled = true;
                            break;
                        }
                    }
                    if (!isFilled) {
                        for (TileRecord record : mRecords) {
                            // check if this is our special tile - can't touch it
                            if (record.equals(tr)) {
                                continue;
                            }

                            // check if the record is outside of the area of interest
                            if (record.row < r || (record.row == r && record.col < c) ||
                                    record.row > row || (record.row == row && record.col > col)) {
                                continue;
                            }

                            // calculate move target
                            int newr = record.row, newc = record.col - 1;
                            if (newc < 0) {
                                newr--;
                                newc = (newr == 0) ? (mDualCount - 1) : (mColumns - 1);
                            }

                            // move is a go
                            record.anim.move(newr, newc);
                        }
                    }
                }
            }

            // collapse after the special tile

            for (int r = mRowCount - 1; r >= row; r--) {
                for (int c = getPlannedColumnCount(r) - 1; c >= (r == row ? col : 0); c--) {
                    boolean isFilled = false;
                    for (TileRecord record : mRecords) {
                        if (record.tileView.getVisibility() == VISIBLE &&
                                record.row == r && record.col == c) {
                            isFilled = true;
                            break;
                        }
                    }
                    if (!isFilled) {
                        for (TileRecord record : mRecords) {
                            // check if this is our special tile - can't touch it
                            if (record.equals(tr)) {
                                continue;
                            }

                            // check if the record is outside of the area of interest
                            if (record.row > r || (record.row == r && record.col > c) ||
                                    record.row < row || (record.row == row && record.col < col)) {
                                continue;
                            }

                            // calculate move target
                            int newr = record.row, newc = record.col + 1;
                            if (newc >= mColumns || (newr == 0 && newc >= mDualCount)) {
                                newr++;
                                newc = 0;
                            }

                            // move is a go
                            record.anim.move(newr, newc);
                        }
                    }
                }
            }

            // grab the record and the tile spec lists

            for (int r = 0; r < mRowCount; r++) {
                for (int c = 0; c < (r == 0 ? mDualCount : mColumns); c++) {
                    for (TileRecord record : mRecords) {
                        if (record.row == r && record.col == c) {
                            records.add(record);
                            specList += record.tile.getSpec() + ",";
                        }
                    }
                }
            }
            for (TileRecord record : mRecords) {
                // and whatever happens, don't lose tiles
                if (!records.contains(record)) {
                    records.add(record);
                    specList += record.tile.getSpec() + ",";
                }
            }
        }

        // trim the extra comma off of the tile spec list, if one exists

        if (specList.length() > 0) {
            specList = specList.substring(0, specList.length() - 1);
        }

        // make sure to apply the changes

        synchronized (mRecords) {
            mRecords.clear();
            mRecords.addAll(records);
        }
        Secure.putStringForUser(mContext.getContentResolver(), Secure.QS_TILES,
                specList, mUserTracker.getCurrentUserId());
    }

    private int countDualTiles() {
        synchronized (mRecords) {
            int dualCount = 0;
            for (TileRecord record : mRecords) {
                if (record.tile.isNativeDualTargets()) {
                    dualCount++;
                }
            }
            return mDualCount = dualCount;
        }
    }

    private int countRows() {
        synchronized (mRecords) {
            int rowCount = 0;
            for (TileRecord record : mRecords) {
                if (rowCount < record.row + 1) {
                    rowCount = record.row + 1;
                }
            }
            return mRowCount = rowCount;
        }
    }

    private void layoutTile(QSTileView tileView, int row, float col) {
        synchronized (tileView) {
            final int w = getWidth();

            tileView.setDual(row == 0);

            final int cw = row == 0 ? mLargeCellWidth : mCellWidth;
            final int ch = row == 0 ? mLargeCellHeight : mCellHeight;
            tileView.measure(exactly(cw), exactly(ch));

            final int cols = getColumnCount(row);
            int left = (int) (col * cw + (col + 1) * ((w - cw * cols) / (cols + 1)));
            final int top = getRowTop(row);
            final int right;
            final int tileWith = tileView.getMeasuredWidth();
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                right = w - left;
                left = right - tileWith;
            } else {
                right = left + tileWith;
            }
            tileView.layout(left, top, right, top + tileView.getMeasuredHeight());
        }
    }

    private void logTiles() {
        for (int i = 0; i < mRecords.size(); i++) {
            TileRecord tileRecord = mRecords.get(i);
            if (tileRecord.tile.getState().visible) {
                MetricsLogger.visible(mContext, tileRecord.tile.getMetricsCategory());
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        mBrightnessView.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        final int brightnessHeight = mBrightnessView.getMeasuredHeight() + mBrightnessPaddingTop;
        mFooter.getView().measure(exactly(width), MeasureSpec.UNSPECIFIED);
        int rows = 0;
        synchronized (mRecords) {
            int dualCount = countDualTiles();
            int r = 0, c = -1;
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() == VISIBLE) {
                    c++;
                    // wrap the column when we reach the column count limit
                    if (c >= mColumns || (r == 0 && c >= dualCount)) {
                        r++;
                        c = 0;
                    }
                }

                if (record.row == -1 || record.col == -1 || !record.anim.isAnimating()) {
                    // move instantly
                    layoutTile(record.tileView, record.uiRow = record.row = r,
                            record.uiCol = record.col = c);
                } else {
                    // animate movement
                    record.anim.move(r, c);
                }
            }
            rows = countRows();
        }
        int h = rows == 0 ? brightnessHeight : (getRowTop(rows) + mPanelPaddingBottom);
        if (mFooter.hasFooter()) {
            h += mFooter.getView().getMeasuredHeight();
        }
        mDetail.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        if (mDetail.getMeasuredHeight() < h) {
            mDetail.measure(exactly(width), exactly(h));
        }
        mGridHeight = h;
        setMeasuredDimension(width, Math.max(h, mDetail.getMeasuredHeight()));
    }

    private static int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getWidth();
        mBrightnessView.layout(0, mBrightnessPaddingTop,
                mBrightnessView.getMeasuredWidth(),
                mBrightnessPaddingTop + mBrightnessView.getMeasuredHeight());
        synchronized (mRecords) {
            countDualTiles();
            for (TileRecord record : mRecords) {
                if (!record.anim.isAnimating()) {
                    layoutTile(record.tileView, record.row, record.col);
                }
            }
        }
        final int dh = Math.max(mDetail.getMeasuredHeight(), getMeasuredHeight());
        mDetail.layout(0, 0, mDetail.getMeasuredWidth(), dh);
        if (mFooter.hasFooter()) {
            View footer = mFooter.getView();
            footer.layout(0, getMeasuredHeight() - footer.getMeasuredHeight(),
                    footer.getMeasuredWidth(), getMeasuredHeight());
        }
    }

    private int getRowTop(int row) {
        if (row <= 0) return mBrightnessView.getMeasuredHeight() + mBrightnessPaddingTop;
        return mBrightnessView.getMeasuredHeight() + mBrightnessPaddingTop
                + mLargeCellHeight - mDualTileUnderlap + (row - 1) * mCellHeight;
    }

    private int getPlannedColumnCount(int row) {
        int cols = 0;
        synchronized (mRecords) {
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() == VISIBLE) {
                    cols++;
                }
            }
        }
        for (int r = 0; r < row; r++) {
            cols -= r == 0 ? mDualCount : mColumns;
            if (cols < 0) {
                return 0;
            }
        }
        final int maxCols = row == 0 ? mDualCount : mColumns;
        return cols > maxCols ? maxCols : cols;
    }

    private int getColumnCount(int row) {
        int cols = 0;
        synchronized (mRecords) {
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() == VISIBLE && record.row == row) {
                    cols++;
                }
            }
        }
        return cols;
    }

    private void fireShowingDetail(QSTile.DetailAdapter detail) {
        if (mCallback != null) {
            mCallback.onShowingDetail(detail);
        }
    }

    private void fireToggleStateChanged(boolean state) {
        if (mCallback != null) {
            mCallback.onToggleStateChanged(state);
        }
    }

    private void fireScanStateChanged(boolean state) {
        if (mCallback != null) {
            mCallback.onScanStateChanged(state);
        }
    }

    private void setDetailRecord(Record r) {
        if (r == mDetailRecord) return;
        mDetailRecord = r;
        final boolean scanState = mDetailRecord instanceof TileRecord
                && ((TileRecord) mDetailRecord).scanState;
        fireScanStateChanged(scanState);
    }

    private class H extends Handler {
        private static final int SHOW_DETAIL = 1;
        private static final int SET_TILE_VISIBILITY = 2;
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_DETAIL) {
                handleShowDetail((Record)msg.obj, msg.arg1 != 0);
            } else if (msg.what == SET_TILE_VISIBILITY) {
                handleSetTileVisibility((View)msg.obj, msg.arg1);
            }
        }
    }

    private static class Record {
        View detailView;
        DetailAdapter detailAdapter;
        int x;
        int y;
    }

    private static final class TileRecord extends Record {
        TileAnimator anim;
        QSTile<?> tile;
        QSTileView tileView;
        int row = -1;
        int col = -1;
        int uiRow = -1;
        int uiCol = -1;
        boolean scanState;
        boolean openingDetail;
    }

    private final class TileAnimator {
        private static final int IN_ACTION_ID = 0;
        private static final int OUT_ACTION_ID = 1;
        private static final int MOVE_ACTION_ID = 2;

        private final LinkedList<TileAnimationAction> mAnimations =
                new LinkedList<TileAnimationAction>();

        private final AnimatorListenerAdapter mListener = new AnimatorListenerAdapter() {
            private void onQueueEnd() {
                synchronized (mSync) {
                    final TileRecord record = getRecord();
                    layoutTile(record.tileView, record.row, record.col);
                    record.tileView.setAlpha(mLiftOngoing ? 0f : 1f);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                synchronized (mSync) {
                    // force-settle post-movement layout before starting the next move
                    final TileRecord record = getRecord();
                    layoutTile(record.tileView, record.row, record.col);

                    TileAnimationAction head = mAnimations.peek();

                    if (head == null) {
                        // there are no animations in the queue whatsoever; abort
                        onQueueEnd();
                        return;
                    }

                    if (animation.equals(head.getAnimation())) {
                        // get rid of head as it just finished
                        mAnimations.poll();
                    }

                    if (mAnimations.peek() == null) {
                        // there is no animation queued up; abort
                        onQueueEnd();
                        return;
                    }

                    // rebuild the whole list in a shortened format

                    final LinkedList<TileAnimationAction> taas =
                            new LinkedList<TileAnimationAction>();

                    TileAnimationAction lastTaa = mAnimations.poll();
                    while (lastTaa != null) {
                        TileAnimationAction taa = mAnimations.poll();

                        if (taa == null) {
                            // we are done with all TAAs
                            taas.add(lastTaa);
                        } else if (lastTaa.actionId != taa.actionId) {
                            // the action ID has changed
                            taas.add(lastTaa);
                        }

                        lastTaa = taa;
                    }

                    mAnimations.addAll(taas);
                    startNext();
                }
            }
        };

        private final Object mSync = new Object();

        private final WeakReference<TileRecord> mRecordRef;

        private boolean mLiftOngoing = false;

        public TileAnimator(TileRecord record) {
            mRecordRef = new WeakReference<TileRecord>(record);
        }

        private TileRecord getRecord() {
            final TileRecord record = mRecordRef.get();
            if (record == null) {
                throw new IllegalStateException("This tile reference has been cleared.");
            }
            if (record.anim != this) {
                throw new IllegalStateException("This tile animator has been dereferenced.");
            }
            return record;
        }

        public boolean isAnimating() {
            synchronized (mSync) {
                final TileAnimationAction q = mAnimations.peek();

                if (q != null) {
                    if (q.getAnimation() == null) {
                        startNext();
                    }

                    return true;
                }

                return false;
            }
        }

        private void startNext() {
            synchronized (mSync) {
                final TileAnimationAction head = mAnimations.peek();

                if (head == null) {
                    // nothing to start
                    return;
                }

                if (head.getAnimation() != null) {
                    // this one has already started so drop this like hot potatoes
                    return;
                }

                if (!head.start()) {
                    // drop this one and try to start the next one instead
                    mAnimations.poll();
                    startNext();
                }
            }
        }

        public void lift() {
            new TileAnimationAction(OUT_ACTION_ID, -1, -1).kickstart();
        }

        public void drop() {
            new TileAnimationAction(IN_ACTION_ID, -1, -1).kickstart();
        }

        public void move(final int tgtRow, final int tgtCol) {
            new TileAnimationAction(MOVE_ACTION_ID, tgtRow, tgtCol).kickstart();
        }

        private final class TileAnimationAction {
            private static final long BASE_ACTION_LENGTH = 300;

            public final int actionId;

            public final int tgtRow;

            public final int tgtCol;

            private Animator animation = null;

            private boolean ksed = false;

            public TileAnimationAction(final int actionId, final int tgtRow, final int tgtCol) {
                this.actionId = actionId;
                this.tgtRow = tgtRow;
                this.tgtCol = tgtCol;
            }

            private ObjectAnimator in(final View view, final long dur) {
                return ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).setDuration(dur);
            }

            private ObjectAnimator out(final View view, final long dur) {
                return ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).setDuration(dur);
            }

            public Animator getAnimation() {
                return animation;
            }

            public void kickstart() {
                synchronized (mSync) {
                    if (ksed) {
                        throw new IllegalStateException("ksed == true");
                    }

                    final TileRecord record = getRecord();
                    if (record.uiRow == -1 || record.uiCol == -1) {
                        record.uiRow = record.row;
                        record.uiCol = record.col;
                    }
                    if (actionId == MOVE_ACTION_ID) {
                        record.row = tgtRow;
                        record.col = tgtCol;
                    }

                    mAnimations.add(this);

                    if (!isAnimating()) {
                        startNext();
                    }

                    ksed = true;
                }
            }

            public boolean start() {
                synchronized (mSync) {
                    if (animation != null) {
                        throw new IllegalStateException("animation != null");
                    }

                    final TileRecord record = getRecord();
                    final QSTileView view = record.tileView;

                    switch (actionId) {
                    case IN_ACTION_ID:
                        if (mLiftOngoing) {
                            mLiftOngoing = false;
                            animation = in(view, BASE_ACTION_LENGTH);
                        } else {
                            // a repeated action that can be ignored
                            return false;
                        }

                        break;

                    case OUT_ACTION_ID:
                        if (mLiftOngoing) {
                            // a repeated action that can be ignored
                            return false;
                        } else {
                            mLiftOngoing = true;
                            animation = out(view, BASE_ACTION_LENGTH);
                        }

                        break;

                    case MOVE_ACTION_ID:
                        final int srcRow = record.uiRow;
                        final int srcCol = record.uiCol;

                        if (srcRow == tgtRow && srcCol == tgtCol) {
                            // nothing to change, nothing to animate
                            return false;
                        }

                        record.uiRow = tgtRow;
                        record.uiCol = tgtCol;

                        if (view.getVisibility() != VISIBLE) {
                            // just layout the tile silently
                            layoutTile(view, tgtRow, tgtCol);
                            return false;
                        }

                        final int w = getWidth();
                        final boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                        final boolean goingDown = srcRow > tgtRow;

                        final AnimatorSet fullSet = new AnimatorSet();
                        Animator lastInSet = null;

                        int row = srcRow;

                        while (goingDown ? (row >= tgtRow) : (row <= tgtRow)) {
                            final int rowSrcCol = row == srcRow ? srcCol :
                                    goingDown ? (row == 0 ? mDualCount : mColumns) : -1;
                            final int rowTgtCol = row == tgtRow ? tgtCol :
                                    goingDown ? -1 : (row == 0 ? mDualCount : mColumns);

                            final ValueAnimator anim =
                                    ValueAnimator.ofFloat((float) rowSrcCol, (float) rowTgtCol);
                            final int animRow = row;
                            anim.setDuration(mLiftOngoing ? 0 :
                                    Math.abs(rowSrcCol - rowTgtCol) * BASE_ACTION_LENGTH);
                            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    layoutTile(view, animRow, (Float) animation.getAnimatedValue());
                                }
                            });

                            if (lastInSet == null) {
                                fullSet.play(anim);
                            } else {
                                fullSet.play(anim).after(lastInSet);
                            }

                            lastInSet = anim;

                            row += goingDown ? -1 : 1;
                        }

                        if (lastInSet == null) {
                            // nothing in set, nothing to animate
                            return false;
                        }

                        animation = fullSet;

                        break;
                    }

                    if (animation == null) {
                        throw new IllegalStateException("animation == null for actionId=" + actionId);
                    }

                    animation.addListener(mListener);
                    animation.start();

                    return true;
                }
            }
        }
    }

    private final AnimatorListenerAdapter mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            mDetailContent.removeAllViews();
            setDetailRecord(null);
            mClosingDetail = false;
        };
    };

    private final AnimatorListenerAdapter mHideGridContentWhenDone = new AnimatorListenerAdapter() {
        public void onAnimationCancel(Animator animation) {
            // If we have been cancelled, remove the listener so that onAnimationEnd doesn't get
            // called, this will avoid accidentally turning off the grid when we don't want to.
            animation.removeListener(this);
            redrawTile();
        };

        @Override
        public void onAnimationEnd(Animator animation) {
            // Only hide content if still in detail state.
            if (mDetailRecord != null) {
                setGridContentVisibility(true);
                redrawTile();
            }
        }

        private void redrawTile() {
            if (mDetailRecord instanceof TileRecord) {
                final TileRecord tileRecord = (TileRecord) mDetailRecord;
                tileRecord.openingDetail = false;
                drawTile(tileRecord, tileRecord.tile.getState());
            }
        }
    };

    public interface Callback {
        void onShowingDetail(QSTile.DetailAdapter detail);
        void onToggleStateChanged(boolean state);
        void onScanStateChanged(boolean state);
    }
}
