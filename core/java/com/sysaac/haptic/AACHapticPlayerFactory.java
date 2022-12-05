package com.sysaac.haptic;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import com.sysaac.haptic.base.ApiInfo;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.player.GooglePlayer;
import com.sysaac.haptic.player.Player;
import com.sysaac.haptic.player.PlayerFactory;
import com.sysaac.haptic.player.RichtapPlayer;
import com.sysaac.haptic.player.TencentPlayer;
import com.sysaac.haptic.player.PlayerEventCallback;
import com.sysaac.haptic.sync.SyncCallback;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AACHapticPlayerFactory implements PlayerFactory {
    private static final boolean DEBUG = false;
    private static final String TAG = "AACHapticPlayerFactory";
    private static final ExecutorService mExcutor = Executors.newSingleThreadExecutor();
    private Context mContext;
    private Player mPlayer;
    private PlayerEventCallback mPlayerEventCallback;

    private AACHapticPlayerFactory() {
    }

    private AACHapticPlayerFactory(Context context, int i) {
        Player c3994b;
        Log.i("AACHapticPlayerFactory", "sdk version:" + ApiInfo.VERSION_NAME + " versionCode:" + ApiInfo.VERSION_CODE + ",playerType:" + i);
        this.mContext = context;
        switch (i) {
            case 0:
                c3994b = new GooglePlayer(context);
                break;
            case 1:
                c3994b = new TencentPlayer(context);
                break;
            case 2:
                c3994b = new RichtapPlayer(context);
                break;
            default:
                Log.w("AACHapticPlayerFactory", "unknown player type:" + i);
                return;
        }
        this.mPlayer = c3994b;
    }

    public static void convertM2VHeToWaveformParams(File file, ArrayList<Long> arrayList, ArrayList<Integer> arrayList2) {
        try {
            Util.m98b(Util.m102b(file), arrayList, arrayList2);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void convertM2VHeToWaveformParams(String str, ArrayList<Long> arrayList, ArrayList<Integer> arrayList2) {
        try {
            Util.m98b(str, arrayList, arrayList2);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static AACHapticPlayerFactory create(Context context) {
        return PlayerFactory.isPlayer(2) ? create(context, 2) : PlayerFactory.isPlayer(1) ? create(context, 1) : create(context, 0);
    }

    public static AACHapticPlayerFactory create(Context context, int i) {
        String str;
        if (Build.VERSION.SDK_INT < 26) {
            str = "OS is lower than Android O, NOT SUPPORTED!";
        } else if (context == null) {
            str = "context == null";
        } else if (PlayerFactory.isPlayer(i)) {
            return new AACHapticPlayerFactory(context, i);
        } else {
            str = "specified player type not available!";
        }
        Log.e("AACHapticPlayerFactory", str);
        return null;
    }

    public static boolean isSupportedRichTap() {
        return RichtapPlayer.m56j() || TencentPlayer.m16j();
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public int getCurrentPosition() {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return 0;
        }
        return player.mo22f();
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public int getDuration() {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return 0;
        }
        return player.mo20g();
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public boolean isPlaying() {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return false;
        }
        return player.mo18h();
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void pause() {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return;
        }
        player.mo31c();
        PlayerEventCallback playerEventCallback = this.mPlayerEventCallback;
        if (playerEventCallback == null) {
            return;
        }
        playerEventCallback.onPlayerStateChanged(7);
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void prepare() {
        PlayerEventCallback playerEventCallback;
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
        } else if (!player.mo24e() || (playerEventCallback = this.mPlayerEventCallback) == null) {
        } else {
            playerEventCallback.onPlayerStateChanged(5);
        }
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void registerPlayerEventCallback(PlayerEventCallback playerEventCallback) {
        this.mPlayerEventCallback = playerEventCallback;
        this.mPlayer.mo49a(playerEventCallback);
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void release() {
        Player player = this.mPlayer;
        if (player != null) {
            player.mo36b();
        }
        PlayerEventCallback playerEventCallback = this.mPlayerEventCallback;
        if (playerEventCallback != null) {
            playerEventCallback.onPlayerStateChanged(1);
        }
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void reset() {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return;
        }
        player.mo53a();
        PlayerEventCallback playerEventCallback = this.mPlayerEventCallback;
        if (playerEventCallback == null) {
            return;
        }
        playerEventCallback.onPlayerStateChanged(0);
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void seekTo(int i) {
        PlayerEventCallback playerEventCallback;
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
        } else if (!player.mo52a(i) || (playerEventCallback = this.mPlayerEventCallback) == null) {
        } else {
            playerEventCallback.onSeekCompleted(i);
        }
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void setDataSource(File file, int i, int i2, SyncCallback syncCallback) {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return;
        }
        player.mo42a(file, i, i2, syncCallback);
        PlayerEventCallback playerEventCallback = this.mPlayerEventCallback;
        if (playerEventCallback == null) {
            return;
        }
        playerEventCallback.onPlayerStateChanged(3);
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void setDataSource(String str, int i, int i2, SyncCallback syncCallback) {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return;
        }
        player.mo32b(str, i, i2, syncCallback);
        PlayerEventCallback playerEventCallback = this.mPlayerEventCallback;
        if (playerEventCallback == null) {
            return;
        }
        playerEventCallback.onPlayerStateChanged(3);
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void setLooping(boolean z) {
        Player player = this.mPlayer;
        if (player != null) {
            player.mo38a(z);
        }
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void start() {
        PlayerEventCallback playerEventCallback;
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
        } else if (!player.mo27d() || (playerEventCallback = this.mPlayerEventCallback) == null) {
        } else {
            playerEventCallback.onPlayerStateChanged(6);
        }
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void stop() {
        Player player = this.mPlayer;
        if (player == null) {
            Log.e("AACHapticPlayerFactory", "null == mPlayer!");
            return;
        }
        player.mo53a();
        PlayerEventCallback playerEventCallback = this.mPlayerEventCallback;
        if (playerEventCallback == null) {
            return;
        }
        playerEventCallback.onPlayerStateChanged(8);
    }

    @Override // com.sysaac.haptic.player.PlayerFactory
    public void unregisterPlayerEventCallback() {
        this.mPlayerEventCallback = null;
    }
}
