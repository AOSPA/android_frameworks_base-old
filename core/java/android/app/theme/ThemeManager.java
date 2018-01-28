package android.app.theme;

import android.content.Context;
import android.content.om.OverlayInfo;
import android.os.RemoteException;

/**
 * A class that handles theme changes
 * <p>
 * Use {@link android.content.Context#getSystemService(java.lang.String)}
 * with argument {@link android.content.Context#THEME_SERVICE} to get
 * an instance of this class.
 *
 * Usage: Get an instance of this class.
 * Call {@link updateTheme(String, String)} to compile, install and enable an overlay.
 * {@link updateTheme(String, String)} will return true if building,
 * installing and enabling the overlay was a success. False otherwise.
 *
 * @author Anass Karbila
 * @hide
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";

    private Context mContext;
    private IThemeService mService;

    public ThemeManager(Context context, IThemeService service) {
        mContext = context;
        mService = service;
    }

    /**
     * Updates the theme with a new accent color
     * @param oi the {@link android.content.om.OverlayInfo} object for the overlay
     * @param accentColor the new accent color of the overlay
     * @return whether this operation was a success
     */
    public boolean updateTheme(OverlayInfo oi, String accentColor) {
        try {
            return mService.updateTheme(oi, accentColor);
        } catch (RemoteException e) {
            return false;
        }
    }
}
