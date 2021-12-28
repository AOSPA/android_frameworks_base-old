package co.aospa.android.systemui;

import android.content.Context;

import co.aospa.android.systemui.dagger.AospaGlobalRootComponent;
import co.aospa.android.systemui.dagger.DaggerAospaGlobalRootComponent;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.dagger.GlobalRootComponent;

public class AospaSystemUIFactory extends SystemUIFactory {
    @Override
    protected GlobalRootComponent buildGlobalRootComponent(Context context) {
        return DaggerAospaGlobalRootComponent.builder()
                .context(context)
                .build();
    }
}
