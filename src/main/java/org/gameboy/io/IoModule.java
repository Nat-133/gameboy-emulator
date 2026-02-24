package org.gameboy.io;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.display.Display;

public class IoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(KeyboardInputHandler.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    WindowDisplay provideWindowDisplay() {
        float[][] palette = {
            {224f / 255f, 248f / 255f, 208f / 255f},  // lightest
            {136f / 255f, 192f / 255f,  70f / 255f},
            { 52f / 255f, 104f / 255f,  50f / 255f},
            {  8f / 255f,  24f / 255f,  32f / 255f},   // darkest
        };
        return new WindowDisplay(palette);
    }

    @Provides
    @Singleton
    Display provideDisplay(WindowDisplay windowDisplay) {
        return windowDisplay;
    }
}
