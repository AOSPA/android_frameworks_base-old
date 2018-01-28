package android.app.theme;

import android.content.om.OverlayInfo;

/** @hide */
interface ITHemeService {

    // Update or install a new theme
    boolean updateTheme(in OverlayInfo oi, String accentColor);
}
