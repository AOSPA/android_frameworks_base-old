/*
 * Copyright (C) 2020 The Android Open Source Project
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
package android.window;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityThread;
import android.app.IWindowToken;
import android.app.ResourcesManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManager.LayoutParams.WindowType;
import android.view.WindowManagerGlobal;

import com.android.internal.annotations.VisibleForTesting;

import java.lang.ref.WeakReference;

/**
 * This class is used to receive {@link Configuration} changes from the associated window manager
 * node on the server side, and apply the change to the {@link Context#getResources() associated
 * Resources} of the attached {@link Context}. It is also used as
 * {@link Context#getWindowContextToken() the token of non-Activity UI Contexts}.
 *
 * @see WindowContext
 * @see android.view.IWindowManager#attachWindowContextToDisplayArea(IBinder, int, int, Bundle)
 *
 * @hide
 */
public class WindowTokenClient extends IWindowToken.Stub {
    /**
     * Attached {@link Context} for this window token to update configuration and resources.
     * Initialized by {@link #attachContext(Context)}.
     */
    private WeakReference<Context> mContextRef = null;

    private final ResourcesManager mResourcesManager = ResourcesManager.getInstance();

    private IWindowManager mWms;

    private final Configuration mConfiguration = new Configuration();

    private boolean mShouldDumpConfigForIme;

    private boolean mAttachToWindowContainer;

    /**
     * Attaches {@code context} to this {@link WindowTokenClient}. Each {@link WindowTokenClient}
     * can only attach one {@link Context}.
     * <p>This method must be called before invoking
     * {@link android.view.IWindowManager#attachWindowContextToDisplayArea(IBinder, int, int,
     * Bundle)}.<p/>
     *
     * @param context context to be attached
     * @throws IllegalStateException if attached context has already existed.
     */
    public void attachContext(@NonNull Context context) {
        if (mContextRef != null) {
            throw new IllegalStateException("Context is already attached.");
        }
        mContextRef = new WeakReference<>(context);
    }

    /**
     * Attaches this {@link WindowTokenClient} to a {@link com.android.server.wm.DisplayArea}.
     *
     * @param type The window type of the {@link WindowContext}
     * @param displayId The {@link Context#getDisplayId() ID of display} to associate with
     * @param options The window context launched option
     * @return {@code true} if attaching successfully.
     */
    public boolean attachToDisplayArea(@WindowType int type, int displayId,
            @Nullable Bundle options) {
        try {
            final Configuration configuration = getWindowManagerService()
                    .attachWindowContextToDisplayArea(this, type, displayId, options);
            if (configuration == null) {
                return false;
            }
            onConfigurationChanged(configuration, displayId);
            mAttachToWindowContainer = true;
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Attaches this {@link WindowTokenClient} to a {@code DisplayContent}.
     *
     * @param displayId The {@link Context#getDisplayId() ID of display} to associate with
     * @return {@code true} if attaching successfully.
     */
    public boolean attachToDisplayContent(int displayId) {
        final IWindowManager wms = getWindowManagerService();
        // #createSystemUiContext may call this method before WindowManagerService is initialized.
        if (wms == null) {
            return false;
        }
        try {
            final Configuration configuration = wms.attachToDisplayContent(this, displayId);
            if (configuration == null) {
                return false;
            }
            onConfigurationChanged(configuration, displayId);
            mAttachToWindowContainer = true;
            return true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Attaches this {@link WindowTokenClient} to a {@code windowToken}.
     *
     * @param windowToken the window token to associated with
     */
    public void attachToWindowToken(IBinder windowToken) {
        try {
            getWindowManagerService().attachWindowContextToWindowToken(this, windowToken);
            mAttachToWindowContainer = true;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /** Detaches this {@link WindowTokenClient} from associated WindowContainer if there's one. */
    public void detachFromWindowContainerIfNeeded() {
        if (!mAttachToWindowContainer) {
            return;
        }
        try {
            getWindowManagerService().detachWindowContextFromWindowContainer(this);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private IWindowManager getWindowManagerService() {
        if (mWms == null) {
            mWms = WindowManagerGlobal.getWindowManagerService();
        }
        return mWms;
    }

    /**
     * Called when {@link Configuration} updates from the server side receive.
     *
     * @param newConfig the updated {@link Configuration}
     * @param newDisplayId the updated {@link android.view.Display} ID
     */
    @VisibleForTesting
    @Override
    public void onConfigurationChanged(Configuration newConfig, int newDisplayId) {
        final Context context = mContextRef.get();
        if (context == null) {
            return;
        }
        final int currentDisplayId = context.getDisplayId();
        final boolean displayChanged = newDisplayId != currentDisplayId;
        final Configuration config = context.getResources().getConfiguration();
        final boolean configChanged = config.diff(newConfig) != 0;
        if (displayChanged || configChanged) {
            // TODO(ag/9789103): update resource manager logic to track non-activity tokens
            mResourcesManager.updateResourcesForActivity(this, newConfig, newDisplayId);
            if (context instanceof WindowContext) {
                ActivityThread.currentActivityThread().getHandler().post(
                        () -> ((WindowContext) context).dispatchConfigurationChanged(newConfig));
            }
        }
        if (displayChanged) {
            context.updateDisplay(newDisplayId);
        }
    }

    @Override
    public void onWindowTokenRemoved() {
        final Context context = mContextRef.get();
        if (context != null) {
            context.destroy();
            mContextRef.clear();
        }
    }
}
