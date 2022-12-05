package com.sysaac.haptic.base;

import android.content.Context;
import java.io.File;

public abstract class PatternHe {

    private static PatternHe sPatternHeImpl = null;

    public static int m163a(int i, int i2) {
        if (i2 < 41 || i2 > 68) {
            if (i > 0 && i < 50) {
                return 10;
            }
            return (i < 50 || i > 100) ? 0 : 15;
        } else if (i > 0 && i < 50) {
            return 15;
        } else {
            if (i >= 50 && i < 75) {
                return 20;
            }
            return (i < 75 || i > 100) ? 0 : 30;
        }
    }

    public static PatternHe m162a(Context context) {
        if (sPatternHeImpl == null) {
            synchronized (PatternHe.class) {
                if (sPatternHeImpl == null) {
                    sPatternHeImpl = new PatternHeImpl(context);
                }
            }
        }
        return sPatternHeImpl;
    }

    public abstract int mo156a(String str);

    public abstract void mo161a();

    public abstract void mo160a(int i);

    public abstract void mo159a(int i, int i2, int i3);

    public abstract void mo157a(File file, int i, int i2, int i3, int i4);

    public abstract void mo155a(String str, int i, int i2, int i3, int i4);

    public abstract void mo153b(int i);

    public abstract void mo152b(int i, int i2);

    public abstract void mo150b(File file, int i, int i2, int i3, int i4);

    public abstract void mo148b(String str, int i, int i2, int i3, int i4);

    public abstract void mo146c(int i);

    public abstract void mo143c(String str, int i, int i2, int i3, int i4);

    public abstract void mo141d(String str, int i, int i2, int i3, int i4);
}
