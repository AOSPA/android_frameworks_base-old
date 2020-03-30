package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.android.systemui.R;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.screenrecord.RecordingController;

public class ScreenRecordTile extends QSTileImpl<QSTile.BooleanState> implements RecordingController.RecordingStateChangeCallback {

    private static final String LOG_TAG = "ScreenRecordTile";

    private Callback mCallback;
    private RecordingController mController;

    public long mMillisUntilFinished = 0;

    public Intent getLongClickIntent() {
        return null;
    }

    public int getMetricsCategory() {
        return 0;
    }

    public ScreenRecordTile(QSHost host, RecordingController controller) {
        super(host);
        mCallback = new Callback();
        mController = controller;
        controller.observe((LifecycleOwner) this, mCallback);
    }

    @Override
    public BooleanState newTileState() {
        QSTile.BooleanState state = new QSTile.BooleanState();
        state.label = mContext.getString(R.string.quick_settings_screen_record_label);
        state.handlesLongClick = false;
        return state;
    }

    @Override
    public void handleClick() {
        if (mController.isStarting()) {
            cancelCountdown();
        } else if (mController.isRecording()) {
            stopRecording();
        } else {
            startCountdown();
        }
        refreshState();
    }

    @Override
    public void handleUpdateState(QSTile.BooleanState state, Object arg) {
        boolean isStarting = mController.isStarting();
        boolean isRecording = mController.isRecording();
        state.value = isRecording || isStarting;
        state.state = (isRecording || isStarting) ? Tile.STATE_ACTIVE  : Tile.STATE_INACTIVE;
        if (isRecording) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_screenrecord);
            state.secondaryLabel = mContext.getString(R.string.quick_settings_screen_record_stop);
        } else if (isStarting) {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_screenrecord);
            state.secondaryLabel = String.format("%d...", new Object[]{Integer.valueOf((int) Math.floorDiv(mMillisUntilFinished + 500, 1000))});
        } else {
            state.icon = QSTileImpl.ResourceIcon.get(R.drawable.ic_qs_screenrecord);
            state.secondaryLabel = mContext.getString(R.string.quick_settings_screen_record_start);
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_screen_record_label);
    }

    private void startCountdown() {
        Log.d(LOG_TAG, "Starting countdown");
        getHost().collapsePanels();
        mController.launchRecordPrompt();
    }

    private void cancelCountdown() {
        Log.d(LOG_TAG, "Cancelling countdown");
        mController.cancelCountdown();
    }

    private void stopRecording() {
        Log.d(LOG_TAG, "Stopping recording from tile");
        mController.stopRecording();
    }

    private final class Callback implements RecordingController.RecordingStateChangeCallback {

        private Callback() {
            // no op
        }

        @Override
        public void onCountdown(long millisUntilFinished) {
            mMillisUntilFinished = millisUntilFinished;
            ScreenRecordTile.refreshState();
        }

        @Override
        public void onCountdownEnd() {
            ScreenRecordTile.refreshState();
        }

        @Override
        public void onRecordingStart() {
            ScreenRecordTile.refreshState();
        }

        @Override
        public void onRecordingEnd() {
            ScreenRecordTile.refreshState();
        }
    }
}
