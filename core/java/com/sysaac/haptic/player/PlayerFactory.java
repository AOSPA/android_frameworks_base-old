package com.sysaac.haptic.player;

import com.sysaac.haptic.sync.SyncCallback;
import java.io.File;

public interface PlayerFactory {
    static boolean isPlayer(int i) {
        switch (i) {
            case 0:
                return true;
            case 1:
                return TencentPlayer.m16j();
            case 2:
                return RichtapPlayer.m56j();
            default:
                return false;
        }
    }

    int getCurrentPosition();

    int getDuration();

    boolean isPlaying();

    void pause();

    void prepare();

    void registerPlayerEventCallback(PlayerEventCallback playerEventCallback);

    void release();

    void reset();

    void seekTo(int i);

    void setDataSource(File file, int i, int i2, SyncCallback syncCallback);

    void setDataSource(String str, int i, int i2, SyncCallback syncCallback);

    void setLooping(boolean z);

    void start();

    void stop();

    void unregisterPlayerEventCallback();
}
