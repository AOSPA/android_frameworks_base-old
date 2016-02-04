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

package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.Settings.Secure;
import android.os.Process;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.tiles.AirplaneModeTile;
import com.android.systemui.qs.tiles.BluetoothTile;
import com.android.systemui.qs.tiles.CastTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.ColorInversionTile;
import com.android.systemui.qs.tiles.DndTile;
import com.android.systemui.qs.tiles.FlashlightTile;
import com.android.systemui.qs.tiles.HotspotTile;
import com.android.systemui.qs.tiles.ImmersiveTile;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.qs.tiles.LocationTile;
import com.android.systemui.qs.tiles.RotationLockTile;
import com.android.systemui.qs.tiles.WifiTile;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.CastController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.RotationLockController;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.provider.Settings.Secure.QS_TILES;

/** Platform implementation of the quick settings tile host **/
public class QSTileHost implements QSTile.Host, Tunable {
    private static final String TAG = "QSTileHost";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Context mContext;
    private final PhoneStatusBar mStatusBar;
    private final LinkedHashMap<String, QSTile<?>> mTiles = new LinkedHashMap<>();
    private final LinkedHashMap<String, QSTile<?>> mHiddenTiles = new LinkedHashMap<>();
    private final BluetoothController mBluetooth;
    private final LocationController mLocation;
    private final RotationLockController mRotation;
    private final NetworkController mNetwork;
    private final ZenModeController mZen;
    private final HotspotController mHotspot;
    private final CastController mCast;
    private final Looper mLooper;
    private final FlashlightController mFlashlight;
    private final UserSwitcherController mUserSwitcherController;
    private final KeyguardMonitor mKeyguard;
    private final SecurityController mSecurity;

    private Callback mCallback;

    public QSTileHost(Context context, PhoneStatusBar statusBar,
            BluetoothController bluetooth, LocationController location,
            RotationLockController rotation, NetworkController network,
            ZenModeController zen, HotspotController hotspot,
            CastController cast, FlashlightController flashlight,
            UserSwitcherController userSwitcher, KeyguardMonitor keyguard,
            SecurityController security) {
        mContext = context;
        mStatusBar = statusBar;
        mBluetooth = bluetooth;
        mLocation = location;
        mRotation = rotation;
        mNetwork = network;
        mZen = zen;
        mHotspot = hotspot;
        mCast = cast;
        mFlashlight = flashlight;
        mUserSwitcherController = userSwitcher;
        mKeyguard = keyguard;
        mSecurity = security;

        final HandlerThread ht = new HandlerThread(QSTileHost.class.getSimpleName(),
                Process.THREAD_PRIORITY_BACKGROUND);
        ht.start();
        mLooper = ht.getLooper();

        TunerService.get(mContext).addTunable(this, QS_TILES);
    }

    public void destroy() {
        TunerService.get(mContext).removeTunable(this);
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public QSTile<?>[] getTiles() {
        final Collection<QSTile<?>> col = mTiles.values();
        return col.toArray(new QSTile<?>[col.size()]);
    }

    @Override
    public QSTile<?>[] getHiddenTiles() {
        final Collection<QSTile<?>> col = mHiddenTiles.values();
        return col.toArray(new QSTile<?>[col.size()]);
    }

    @Override
    public void startActivityDismissingKeyguard(final Intent intent) {
        mStatusBar.postStartActivityDismissingKeyguard(intent, 0);
    }

    @Override
    public void startActivityDismissingKeyguard(PendingIntent intent) {
        mStatusBar.postStartActivityDismissingKeyguard(intent);
    }

    @Override
    public void warn(String message, Throwable t) {
        // already logged
    }

    @Override
    public void collapsePanels() {
        mStatusBar.postAnimateCollapsePanels();
    }

    @Override
    public Looper getLooper() {
        return mLooper;
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public BluetoothController getBluetoothController() {
        return mBluetooth;
    }

    @Override
    public LocationController getLocationController() {
        return mLocation;
    }

    @Override
    public RotationLockController getRotationLockController() {
        return mRotation;
    }

    @Override
    public NetworkController getNetworkController() {
        return mNetwork;
    }

    @Override
    public ZenModeController getZenModeController() {
        return mZen;
    }

    @Override
    public HotspotController getHotspotController() {
        return mHotspot;
    }

    @Override
    public CastController getCastController() {
        return mCast;
    }

    @Override
    public FlashlightController getFlashlightController() {
        return mFlashlight;
    }

    @Override
    public KeyguardMonitor getKeyguardMonitor() {
        return mKeyguard;
    }

    public UserSwitcherController getUserSwitcherController() {
        return mUserSwitcherController;
    }

    public SecurityController getSecurityController() {
        return mSecurity;
    }

    private void destroySpareTiles(final LinkedHashMap<String, QSTile<?>> map,
            final TileSpecsWrapper newTileSpecs) {
        for (final Map.Entry<String, QSTile<?>> tile : map.entrySet()) {
            if (!newTileSpecs.contains(tile.getKey()) &&
                    !newTileSpecs.containsHidden(tile.getKey())) {
                if (DEBUG) Log.d(TAG, "Destroying tile: " + tile.getKey());
                tile.getValue().destroy();
            }
        }
    }

    private LinkedHashMap<String, QSTile<?>> buildTiles(final String[] newTileSpecsList) {
        final LinkedHashMap<String, QSTile<?>> newTiles = new LinkedHashMap<>();
        for (final String tileSpec : newTileSpecsList) {
            if (mTiles.containsKey(tileSpec)) {
                newTiles.put(tileSpec, mTiles.get(tileSpec));
            } else if (mHiddenTiles.containsKey(tileSpec)) {
                newTiles.put(tileSpec, mHiddenTiles.get(tileSpec));
            } else {
                if (DEBUG) Log.d(TAG, "Creating tile: " + tileSpec);
                try {
                    newTiles.put(tileSpec, createTile(tileSpec));
                } catch (final Throwable t) {
                    Log.w(TAG, "Error creating tile for spec: " + tileSpec, t);
                }
            }
        }
        return newTiles;
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!QS_TILES.equals(key)) return;
        if (DEBUG) Log.d(TAG, "Recreating tiles");

        final TileSpecsWrapper tileSpecs = loadTileSpecs();

        destroySpareTiles(mTiles, tileSpecs);
        destroySpareTiles(mHiddenTiles, tileSpecs);

        final LinkedHashMap<String, QSTile<?>> newTiles = buildTiles(tileSpecs.get());
        final LinkedHashMap<String, QSTile<?>> newHiddenTiles = buildTiles(tileSpecs.getHidden());

        final boolean tilesChanged = !Arrays.equals(mTiles.keySet().toArray(),
                newTiles.keySet().toArray());
        final boolean hiddenTilesChanged = !Arrays.equals(mHiddenTiles.keySet().toArray(),
                newHiddenTiles.keySet().toArray());

        if (!tilesChanged && !hiddenTilesChanged) return;
        mTiles.clear();
        mTiles.putAll(newTiles);
        mHiddenTiles.clear();
        mHiddenTiles.putAll(newHiddenTiles);
        if (mCallback != null) {
            mCallback.onTilesChanged();
        }
    }

    private QSTile<?> createTile(String tileSpec) {
        if (tileSpec.equals(WifiTile.SPEC)) return new WifiTile(this);
        else if (tileSpec.equals(BluetoothTile.SPEC)) return new BluetoothTile(this);
        else if (tileSpec.equals(ColorInversionTile.SPEC)) return new ColorInversionTile(this);
        else if (tileSpec.equals(CellularTile.SPEC)) return new CellularTile(this);
        else if (tileSpec.equals(AirplaneModeTile.SPEC)) return new AirplaneModeTile(this);
        else if (tileSpec.equals(DndTile.SPEC)) return new DndTile(this);
        else if (tileSpec.equals(RotationLockTile.SPEC)) return new RotationLockTile(this);
        else if (tileSpec.equals(FlashlightTile.SPEC)) return new FlashlightTile(this);
        else if (tileSpec.equals(LocationTile.SPEC)) return new LocationTile(this);
        else if (tileSpec.equals(CastTile.SPEC)) return new CastTile(this);
        else if (tileSpec.equals(HotspotTile.SPEC)) return new HotspotTile(this);
        else if (tileSpec.equals(ImmersiveTile.SPEC)) return new ImmersiveTile(this);
        else if (tileSpec.startsWith(IntentTile.PREFIX)) return IntentTile.create(this,tileSpec);
        else throw new IllegalArgumentException("Bad tile spec: " + tileSpec);
    }

    public TileSpecsWrapper loadTileSpecs() {
        final TileSpecsWrapper tileSpecs = new TileSpecsWrapper();

        final Resources res = mContext.getResources();
        final String[] defaultParts = res.getString(R.string.quick_settings_tiles_default)
                .split(",,");
        final String defaultTiles = defaultParts[0];
        final String defaultHiddenTiles = defaultParts.length > 1 ? defaultParts[1] : "";
        if (DEBUG) Log.d(TAG, "Loaded default tile specs: [" + defaultTiles + "]" +
                "[" + defaultHiddenTiles + "]");

        final String setting = Secure.getStringForUser(mContext.getContentResolver(), QS_TILES,
                ActivityManager.getCurrentUser());
        final String tiles, hiddenTiles;
        if (setting != null) {
            final String[] settingParts = setting.split(",,");
            tiles = settingParts[0];
            hiddenTiles = settingParts.length > 1 ? settingParts[1] : "";
            if (DEBUG) Log.d(TAG, "Using pre-existing tile specs: [" + tiles + "]" +
                    "[" + hiddenTiles + "]");
        } else {
            tiles = "default";
            hiddenTiles = "";
            if (DEBUG) Log.d(TAG, "No pre-existing tile specs found. Using defaults.");
        }

        for (String tile : tiles.split(",")) {
            tile = tile.trim();
            tileSpecs.add(tile);
        }

        for (String hiddenTile : hiddenTiles.split(",")) {
            hiddenTile = hiddenTile.trim();
            tileSpecs.addHidden(hiddenTile);
        }

        for (String defaultTile : defaultTiles.split(",")) {
            defaultTile = defaultTile.trim();
            if (!tileSpecs.contains(defaultTile) &&
                    !tileSpecs.containsHidden(defaultTile)) {
                tileSpecs.add(defaultTile);
            }
        }

        for (String defaultHiddenTile : defaultHiddenTiles.split(",")) {
            defaultHiddenTile = defaultHiddenTile.trim();
            if (!tileSpecs.contains(defaultHiddenTile) &&
                    !tileSpecs.containsHidden(defaultHiddenTile)) {
                tileSpecs.addHidden(defaultHiddenTile);
            }
        }

        return tileSpecs;
    }

    private final class TileSpecsWrapper {
        public final ArrayList<String> list = new ArrayList<>();
        public final ArrayList<String> hiddenList = new ArrayList<>();

        public TileSpecsWrapper() {
            // no-op
        }

        public void add(final String tile) {
            if (tile.isEmpty()) return;
            synchronized (list) {
                list.add(tile);
            }
        }

        public void addHidden(final String hiddenTile) {
            if (hiddenTile.isEmpty()) return;
            synchronized (hiddenList) {
                hiddenList.add(hiddenTile);
            }
        }

        public boolean contains(final String tile) {
            if (tile.isEmpty()) return true;
            synchronized (list) {
                return list.contains(tile);
            }
        }

        public boolean containsHidden(final String hiddenTile) {
            if (hiddenTile.isEmpty()) return true;
            synchronized (hiddenList) {
                return hiddenList.contains(hiddenTile);
            }
        }

        public String[] get() {
            synchronized (list) {
                return list.toArray(new String[list.size()]);
            }
        }

        public String[] getHidden() {
            synchronized (hiddenList) {
                return hiddenList.toArray(new String[hiddenList.size()]);
            }
        }
    }
}
