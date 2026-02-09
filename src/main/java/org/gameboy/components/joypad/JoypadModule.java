package org.gameboy.components.joypad;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.gameboy.components.joypad.annotations.*;

public class JoypadModule extends AbstractModule {

    @Override
    protected void configure() {
        // ButtonGroups and JoypadController are auto-injected via @Inject constructors
    }

    // D-pad buttons
    @Provides @Singleton @ButtonRight
    Button provideRightButton() {
        return new Button();
    }

    @Provides @Singleton @ButtonLeft
    Button provideLeftButton() {
        return new Button();
    }

    @Provides @Singleton @ButtonUp
    Button provideUpButton() {
        return new Button();
    }

    @Provides @Singleton @ButtonDown
    Button provideDownButton() {
        return new Button();
    }

    // Action buttons
    @Provides @Singleton @ButtonA
    Button provideAButton() {
        return new Button();
    }

    @Provides @Singleton @ButtonB
    Button provideBButton() {
        return new Button();
    }

    @Provides @Singleton @ButtonSelect
    Button provideSelectButton() {
        return new Button();
    }

    @Provides @Singleton @ButtonStart
    Button provideStartButton() {
        return new Button();
    }
}
