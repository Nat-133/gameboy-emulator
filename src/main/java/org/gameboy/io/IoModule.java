package org.gameboy.io;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.display.Display;

import java.awt.*;

public class IoModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(KeyboardInputHandler.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    WindowDisplay provideWindowDisplay() {
        return new WindowDisplay(
            new Color(224, 248, 208),
            new Color(136, 192, 70),
            new Color(52, 104, 50),
            new Color(8, 24, 32)
        );
    }

    @Provides
    @Singleton
    Display provideDisplay(WindowDisplay windowDisplay) {
        return windowDisplay;
    }
}
