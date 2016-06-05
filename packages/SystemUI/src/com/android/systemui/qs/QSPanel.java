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
    private final TextView mHiddenTilesInfo;
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
    private int mHiddenRowCount = 0;
    private int mDualCount = 0;
    private boolean mExpanded;
    private boolean mListening;
    private boolean mClosingDetail;
    private boolean mShowingHidden = false;

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
        mHiddenTilesInfo = (TextView) LayoutInflater.from(context).inflate(
                R.layout.quick_settings_hidden_tiles_info, this, false);
        mHiddenTilesInfo.setAlpha(0);
        mHiddenTilesInfo.setVisibility(INVISIBLE);
        mFooter = new QSFooter(this, context);
        addView(mDetail);
        addView(mBrightnessView);
        addView(mHiddenTilesInfo);
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

        setOnDragListener(new View.OnDragListener() {

            @Override
            public boolean onDrag(final View v, final DragEvent event) {
                final Object localState = event.getLocalState();
                if (!(localState instanceof TileRecord)) {
                    // this isn't even our drag - ignore it
                    return false;
                }
                final TileRecord r = (TileRecord) localState;

                switch(event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true; // just tell the system we can accept this

                case DragEvent.ACTION_DRAG_LOCATION:
                    final float x = event.getX();
                    final float y = event.getY();

                    final boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                    final int w = getWidth();

                    for (int row = 0; row < mRowCount; row++) {
                        final int top = getRowTop(row, false);
                        final int ch = row == 0 && !r.hidden
                                ? mLargeCellHeight : mCellHeight;
                        if (y <= top || y >= top + ch) {
                            continue;
                        }

                        final int cols = getPlannedColumnCount(row, false);
                        final int cw = row == 0 && !r.hidden
                                ? mLargeCellWidth : mCellWidth;

                        for (int col = 0; col < cols; col++) {
                            final int left = getColLeft(row, col, false);
                            if (x <= left || x >= left + cw) {
                                continue;
                            }

                            if (r.row != row || r.col != col) {
                                setTilePosition(r, row, col);
                            }
                            return true;
                        }
                    }

                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DROP:
                    // no-op
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    mCallback.onReorderMode(false);
                    if (r.hidden == mShowingHidden) {
                        r.anim.drop();
                    }
                    setTileInteractive(r, r.hidden == mShowingHidden);
                    return true;
                }

                return false;
            }

        });
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
        if (callback != null) {
            callback.onAbleToShowHidden(mHiddenRowCount != 0);
        }
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
        final int dualCount = Math.max(0, res.getInteger(R.integer.quick_settings_num_dual_tiles));
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
        if (mDualCount != dualCount) {
            mDualCount = dualCount;
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
            showHidden(false);
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
            final QSTile<?>[] newHiddenTiles = mHost.getHiddenTiles();

            if (oldTileRecords.length == (newTiles.length + newHiddenTiles.length)) {
                boolean allMatch = true;
                for (int i = 0; i < oldTileRecords.length; i++) {
                    if (oldTileRecords[i].hidden != (i >= newTiles.length)) {
                        // make sure the ordering of records matches expected hidden states
                        allMatch = false;
                        break;
                    }

                    if (i < newTiles.length) {
                        // this should be a visible tile; make sure it's positioned right
                        if (!oldTileRecords[i].tile.equals(newTiles[i])) {
                            allMatch = false;
                            break;
                        }
                    } else {
                        // this should be a hidden tile; just make sure it is still hidden
                        boolean matchFound = false;
                        for (final QSTile<?> t : newHiddenTiles) {
                            if (oldTileRecords[i].tile.equals(t)) {
                                matchFound = true;
                                break;
                            }
                        }
                        if (!matchFound) {
                            allMatch = false;
                            break;
                        }
                    }
                }
                if (allMatch) {
                    // the resulting arrays and hidden states match up exactly; just return
                    return;
                }
            }

            for (TileRecord record : mRecords) {
                removeView(record.tileView);
            }
            mRecords.clear();

            for (QSTile<?> tile : newTiles) {
                addTile(tile, false);
            }
            for (QSTile<?> tile : newHiddenTiles) {
                addTile(tile, true);
            }
        }

        if (isShowingDetail()) {
            mDetail.bringToFront();
        }
    }

    public void hideTile(final Object o) {
        if (!(o instanceof TileRecord)) return;
        synchronized (mRecords) {
            final TileRecord r = (TileRecord) o;

            final int oldLastRow = mRowCount - 1;
            setTilePosition(r, oldLastRow, getPlannedColumnCount(oldLastRow, false));

            final int oldRowCount = mRowCount;

            mCallback.onReorderMode(false);
            r.hidden = true;
            setTileInteractive(r, mShowingHidden);

            final int lastRow = countRows() - 1;

            if (r.row >= lastRow) {
                // last row has been altered directly; force proper layouts there
                int orc = -1;
                for (final TileRecord or : mRecords) {
                    if (or.row == lastRow && !or.hidden) {
                        if (or.tileView.getVisibility() != GONE) {
                            orc++;
                        }
                        or.anim.move(lastRow, orc);
                    }
                }
            }

            if (mRowCount != oldRowCount) {
                // height requirements have changed; force measurements
                requestLayout();
                postInvalidate();
            }

            r.row = r.col = -1;
            setHiddenTilePositions();
            int newRow = mHiddenRowCount - 1;
            if (newRow < 0) newRow = 0;
            setTilePosition(r, newRow, getPlannedColumnCount(newRow, true) - 1);
        }
    }

    public void showHidden(final boolean show) {
        if (mShowingHidden == show || (show && mHiddenRowCount == 0)) return;

        synchronized (mRecords) {
            if (mShowingHidden == show) return;
            mShowingHidden = show;

            if (isShowingDetail()) {
                closeDetail();
            }

            if (mShowingHidden) {
                setHiddenTilePositions();
            }

            final View showingTopView = mShowingHidden ? mHiddenTilesInfo : mBrightnessView;
            final View hidingTopView = mShowingHidden ? mBrightnessView : mHiddenTilesInfo;

            showingTopView.bringToFront();
            showingTopView.setVisibility(VISIBLE);

            if (showingTopView.hasOverlappingRendering()) {
                showingTopView.animate().withLayer();
            }
            showingTopView.animate().alpha(1)
                    .setDuration(TileAnimator.TileAnimationAction.BASE_ACTION_LENGTH)
                    .start();

            if (hidingTopView.hasOverlappingRendering()) {
                hidingTopView.animate().withLayer();
            }
            hidingTopView.animate().alpha(0)
                    .setDuration(TileAnimator.TileAnimationAction.BASE_ACTION_LENGTH)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            hidingTopView.setVisibility(INVISIBLE);
                        }
                    })
                    .start();

            boolean hiddenTilesFound = false;

            for (TileRecord r : mRecords) {
                if (r.hidden && r.tile.getState().visible) {
                    hiddenTilesFound = true;
                }
                if (r.hidden == mShowingHidden) {
                    r.anim.drop();
                } else {
                    r.anim.lift();
                }
                setTileInteractive(r, r.hidden == mShowingHidden);
            }

            requestLayout();
            postInvalidate();

            mCallback.onShowingHidden(mShowingHidden);
        }
    }

    private void drawTile(TileRecord r, QSTile.State state) {
        final int visibility = state.visible ? VISIBLE : GONE;
        setTileVisibility(r.tileView, visibility);
        r.tileView.onStateChanged(state);
    }

    private void addTile(final QSTile<?> tile, final boolean hidden) {
        final TileRecord r = new TileRecord();
        final boolean visibleNow = hidden == mShowingHidden;
        r.tile = tile;
        r.tileView = tile.createTileView(mContext);
        r.anim = new TileAnimator(r, !visibleNow);
        r.hidden = hidden;
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
        r.tile.setListening(mListening);
        callback.onStateChanged(r.tile.getState());
        r.tile.refreshState();
        synchronized (mRecords) {
            mRecords.add(r);
        }

        addView(r.tileView);
        setTileInteractive(r, visibleNow);
    }

    private void setTileInteractive(final TileRecord r, final boolean interactive) {
        final View.OnClickListener click;
        final View.OnClickListener clickSecondary;
        final View.OnLongClickListener longClick;

        if (interactive) {
            click = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    r.tile.click(r.tileView.isDual());
                }
            };
            clickSecondary = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    r.tile.secondaryClick();
                }
            };
            longClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showHidden(false);

                    if (r.hidden) {
                        // unhide the tile
                        r.hidden = false;
                        setTileInteractive(r, true);
                        r.row = r.col = -1;
                        int newRow = countRows() - 1;
                        if (newRow < 0) newRow = 0;
                        setTilePosition(r, newRow, getPlannedColumnCount(newRow, false) - 1);
                    }

                    r.anim.lift();
                    String tileName = r.tile.getClass().getSimpleName();
                    r.tileView.startDrag(ClipData.newPlainText("QSTile." + tileName, tileName),
                            r.tileView.getDragShadowBuilder(), r, 0);
                    mCallback.onReorderMode(true);

                    return true;
                }
            };
        } else {
            click = null;
            clickSecondary = null;
            longClick = null;
        }

        r.tileView.setAlpha(interactive ? 1f : 0f);
        r.tileView.init(click, clickSecondary, longClick);
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
        mHiddenTilesInfo.setVisibility(newVis);
        if (mGridContentVisible != visible) {
            MetricsLogger.visibility(mContext, MetricsLogger.QS_PANEL, newVis);
        }
        mGridContentVisible = visible;
    }

    private void setTilePosition(TileRecord tr, int row, int col) {
        ArrayList<TileRecord> records = new ArrayList<TileRecord>(); // the resulting record list
        String specList = ""; // the resulting tile spec list to be stored
        String hiddenSpecList = ""; // the resulting hidden tile spec list to be stored

        synchronized (mRecords) {
            countRows();
            if (row < 0) {
                row = 0;
            }
            if (col < 0) {
                col = 0;
            }
            if (col >= mColumns || (row == 0 && col >= mDualCount && !tr.hidden)) {
                row++;
                col = 0;
            }

            // move our special tile out of the way first

            tr.anim.move(row, col);

            // collapse before the special tile

            for (int r = 0; r <= row; r++) {
                for (int c = 0; c < (r == row ? (col + 1) : getPlannedColumnCount(r, false)); c++) {
                    boolean isFilled = false;
                    for (TileRecord record : mRecords) {
                        if (record.tileView.getVisibility() != GONE &&
                                record.row == r && record.col == c && !record.hidden) {
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
                                    record.row > row || (record.row == row && record.col > col) ||
                                    record.hidden) {
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
                for (int c = getPlannedColumnCount(r, false) - 1; c >= (r == row ? col : 0); c--) {
                    boolean isFilled = false;
                    for (TileRecord record : mRecords) {
                        if (record.tileView.getVisibility() != GONE &&
                                record.row == r && record.col == c && !record.hidden) {
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
                                    record.row < row || (record.row == row && record.col < col) ||
                                    record.hidden) {
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
                        if (record.row == r && record.col == c && !record.hidden) {
                            records.add(record);
                            specList += record.tile.getSpec() + ",";
                        }
                    }
                }
            }
            for (TileRecord record : mRecords) {
                if (!records.contains(record)) {
                    records.add(record);
                    if (!record.hidden) {
                        record.hidden = true;
                        setTileInteractive(record, mShowingHidden);
                        record.row = record.col = -1;
                        int newRow = countHiddenRows() - 1;
                        if (newRow < 0) newRow = 0;
                        record.anim.move(newRow, getPlannedColumnCount(newRow, true) - 1);
                        countRows();
                    }
                    hiddenSpecList += record.tile.getSpec() + ",";
                }
            }

            // trim the extra commas off of the tile spec lists, if they exists

            if (specList.length() > 0) {
                specList = specList.substring(0, specList.length() - 1);
            }
            if (hiddenSpecList.length() > 0) {
                hiddenSpecList = hiddenSpecList.substring(0, hiddenSpecList.length() - 1);
                specList += ",," + hiddenSpecList;
            }

            // make sure to apply the changes

            mRecords.clear();
            mRecords.addAll(records);
        }
        Secure.putStringForUser(mContext.getContentResolver(), Secure.QS_TILES,
                specList, mUserTracker.getCurrentUserId());
    }

    private int countHiddenRows() {
        synchronized (mRecords) {
            int hiddenRowCount = 0;
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() == GONE) continue;

                if (record.hidden) {
                    if (hiddenRowCount < record.row + 1) {
                        hiddenRowCount = record.row + 1;
                    }
                }
            }
            if ((mHiddenRowCount == 0) != (hiddenRowCount == 0) && mCallback != null) {
                mCallback.onAbleToShowHidden(hiddenRowCount != 0);
            }
            return mHiddenRowCount = hiddenRowCount;
        }
    }

    private int countRows() {
        synchronized (mRecords) {
            countHiddenRows();

            int rowCount = 0;
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() == GONE) continue;

                if (record.hidden) continue;

                if (rowCount < record.row + 1) {
                    rowCount = record.row + 1;
                }
            }
            return mRowCount = rowCount;
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

    private void setHiddenTilePositions() {
        synchronized (mRecords) {
            int hr = 0, hc = -1;
            for (TileRecord record : mRecords) {
                if (!record.hidden) continue;

                if (record.tileView.getVisibility() != GONE) {
                    hc++;
                    // wrap the column when we reach the column count limit
                    if (hc >= mColumns) {
                        hr++;
                        hc = 0;
                    }
                }

                record.anim.move(hr, hc);
            }
            countHiddenRows();
        }
    }

    private void setTilePositions() {
        synchronized (mRecords) {
            setHiddenTilePositions();
            int r = 0, c = -1;
            for (TileRecord record : mRecords) {
                if (record.hidden) continue;

                if (record.tileView.getVisibility() != GONE) {
                    c++;
                    // wrap the column when we reach the column count limit
                    if (c >= mColumns || (r == 0 && c >= mDualCount)) {
                        r++;
                        c = 0;
                    }
                }

                record.anim.move(r, c);
            }
            countRows();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setTilePositions();

        final int rowCount = mShowingHidden ? countHiddenRows() : countRows();
        mGridHeight = getRowTop(rowCount, mShowingHidden)
                + (rowCount > 0 ? mPanelPaddingBottom : 0)
                + (mFooter.hasFooter() ? mFooter.getView().getMeasuredHeight() : 0);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        mBrightnessView.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        mHiddenTilesInfo.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        final int topHeight = mBrightnessView.getMeasuredHeight();
        if (mHiddenTilesInfo.getMeasuredHeight() < topHeight) {
            mHiddenTilesInfo.measure(exactly(width), exactly(topHeight));
        }
        mFooter.getView().measure(exactly(width), MeasureSpec.UNSPECIFIED);
        mDetail.measure(exactly(width), MeasureSpec.UNSPECIFIED);
        if (mDetail.getMeasuredHeight() < mGridHeight) {
            mDetail.measure(exactly(width), exactly(mGridHeight));
        }

        setMeasuredDimension(width, Math.max(mGridHeight, mDetail.getMeasuredHeight()));
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
        mHiddenTilesInfo.layout(0, mBrightnessPaddingTop,
                mHiddenTilesInfo.getMeasuredWidth(),
                mBrightnessPaddingTop + mHiddenTilesInfo.getMeasuredHeight());
        synchronized (mRecords) {
            for (TileRecord record : mRecords) {
                record.anim.move(record.row, record.col);
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

    private int getRowTop(int row, boolean hidden) {
        final View topView = hidden ? mHiddenTilesInfo : mBrightnessView;
        if (row <= 0) return topView.getMeasuredHeight() + mBrightnessPaddingTop;
        return topView.getMeasuredHeight() + mBrightnessPaddingTop + (hidden
                ? (row * mCellHeight)
                : ((mDualCount > 0 ? mLargeCellHeight - mDualTileUnderlap : 0)
                        + (row - 1) * mCellHeight));
    }

    private int getColLeft(int row, int col, boolean hidden) {
        final int cols = getPlannedColumnCount(row, hidden);
        final int w = getWidth();
        final int cw = row == 0 && !hidden ? mLargeCellWidth : mCellWidth;
        final int left = (int) (col * cw + (col + 1) * ((w - cw * cols) / (cols + 1)));
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL ? w - left - cw : left;
    }

    private int getPlannedColumnCount(int row, boolean hidden) {
        if (row < 0) {
            return 0;
        }
        int cols = 0;
        synchronized (mRecords) {
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() != GONE &&
                        record.hidden == hidden) {
                    cols++;
                }
            }
        }
        for (int r = 0; r < row; r++) {
            cols -= r == 0 && !hidden ? mDualCount : mColumns;
            if (cols < 0) {
                return 0;
            }
        }
        final int maxCols = row == 0 && !hidden ? mDualCount : mColumns;
        return cols > maxCols ? maxCols : cols;
    }

    private int getColumnCount(int row, boolean hidden) {
        int cols = 0;
        synchronized (mRecords) {
            for (TileRecord record : mRecords) {
                if (record.tileView.getVisibility() != GONE &&
                        record.row == row && record.hidden == hidden) {
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
        boolean scanState;
        boolean openingDetail;
        boolean hidden;
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
                    record.tileView.setAlpha(mLiftOngoing ? 0f : 1f);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                synchronized (mSync) {
                    // force-settle post-movement layout before starting the next move
                    final TileRecord record = getRecord();

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
            this(record, false);
        }

        public TileAnimator(TileRecord record, boolean liftOngoing) {
            mRecordRef = new WeakReference<TileRecord>(record);
            mLiftOngoing = liftOngoing;
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

        public TileAnimationAction getRunning() {
            synchronized (mSync) {
                final TileAnimationAction q = mAnimations.peek();

                if (q == null) {
                    return null;
                }

                if (q.getAnimation() != null) {
                    return q;
                }

                startNext();
                return getRunning();
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

            private boolean instant = false;

            public TileAnimationAction(final int actionId, final int tgtRow, final int tgtCol) {
                this.actionId = actionId;
                this.tgtRow = tgtRow;
                this.tgtCol = tgtCol;
            }

            private ObjectAnimator in(final View view, final long dur) {
                return ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
                        .setDuration(instant ? 0 : dur);
            }

            private ObjectAnimator out(final View view, final long dur) {
                return ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
                        .setDuration(instant ? 0 : dur);
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
                    if (actionId == MOVE_ACTION_ID) {
                        if (record.row == -1 || record.col == -1) {
                            instant = true;
                        }
                        record.row = tgtRow;
                        record.col = tgtCol;
                    }

                    final TileAnimationAction runningTaa = getRunning();
                    mAnimations.add(this);

                    if (runningTaa == null) {
                        startNext();
                    } else if (runningTaa.actionId == MOVE_ACTION_ID) {
                        runningTaa.animation.cancel();
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
                        final boolean hidden = record.hidden;

                        final int srcTop = view.getTop();
                        final int tgtTop = getRowTop(tgtRow, hidden);
                        int srcRow = -1;
                        for (int row = hidden ? mHiddenRowCount : mRowCount; row >= 0; row--) {
                            if (getRowTop(row, hidden) == srcTop) {
                                srcRow = row;
                                break;
                            }
                        }

                        final int srcLeft = view.getLeft();
                        final int tgtLeft = getColLeft(tgtRow, tgtCol, hidden);
                        int srcCol = -1;
                        for (int col = getPlannedColumnCount(srcRow, hidden); col >= 0; col--) {
                            if (getColLeft(srcRow, col, hidden) == srcLeft) {
                                srcCol = col;
                                break;
                            }
                        }

                        final int width = getWidth();
                        final boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
                        final boolean isGoingDown = srcTop > tgtTop;

                        final boolean rowDual = !hidden && getRowTop(0, false) == srcTop;
                        final int rowSrcLeft = srcLeft;
                        final int rowTgtLeft = srcTop == tgtTop ? tgtLeft : getColLeft(srcRow,
                                isGoingDown ? -1 : (rowDual ? mDualCount : mColumns), hidden);

                        final int rowCellWidth = rowDual ? mLargeCellWidth : mCellWidth;
                        final int rowCellHeight = rowDual ? mLargeCellHeight : mCellHeight;

                        if (mLiftOngoing || view.getVisibility() != VISIBLE) {
                            instant = true;
                        }

                        final ValueAnimator rowAnim = ValueAnimator
                                .ofInt(rowSrcLeft, rowTgtLeft);
                        rowAnim.setDuration(instant ? 0 :
                                Math.abs(rowSrcLeft - rowTgtLeft) *
                                        BASE_ACTION_LENGTH / rowCellWidth);
                        rowAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(final ValueAnimator animation) {
                                synchronized (view) {
                                    view.setDual(rowDual);
                                    view.measure(exactly(rowCellWidth),
                                            exactly(rowCellHeight));
                                    final int left = (Integer) animation.getAnimatedValue();
                                    view.layout(left, srcTop,
                                            left + rowCellWidth, srcTop + rowCellHeight);
                                }
                            }
                        });

                        if (srcTop == tgtTop) {
                            animation = rowAnim;
                        } else {
                            final boolean ndRowDual = !hidden && getRowTop(0, false) == tgtTop;
                            final int ndRowSrcLeft = getColLeft(tgtRow, isGoingDown ?
                                    (ndRowDual ? mDualCount : mColumns) : -1, hidden);
                            final int ndRowTgtLeft = tgtLeft;

                            final int ndRowCellWidth = ndRowDual ? mLargeCellWidth : mCellWidth;
                            final int ndRowCellHeight = ndRowDual ? mLargeCellHeight : mCellHeight;

                            final ValueAnimator ndRowAnim = ValueAnimator
                                    .ofInt(ndRowSrcLeft, ndRowTgtLeft);
                            ndRowAnim.setDuration(instant ? 0 :
                                    Math.abs(ndRowSrcLeft - ndRowTgtLeft) *
                                            BASE_ACTION_LENGTH / ndRowCellWidth);
                            ndRowAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(final ValueAnimator animation) {
                                    synchronized (view) {
                                        view.setDual(ndRowDual);
                                        view.measure(exactly(ndRowCellWidth),
                                                exactly(ndRowCellHeight));
                                        final int left = (Integer) animation.getAnimatedValue();
                                        view.layout(left, tgtTop,
                                                left + ndRowCellWidth, tgtTop + ndRowCellHeight);
                                    }
                                }
                            });

                            final AnimatorSet fullSet = new AnimatorSet();
                            fullSet.play(rowAnim);
                            fullSet.play(ndRowAnim).after(rowAnim);
                            animation = fullSet;
                        }

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
        void onAbleToShowHidden(boolean isAbleToShow);
        void onShowingHidden(boolean isShowingHidden);
        void onReorderMode(boolean isInReorderMode);
        void onToggleStateChanged(boolean state);
        void onScanStateChanged(boolean state);
    }
}
