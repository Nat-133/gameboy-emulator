package org.gameboy.input;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class InputModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(KeyboardInputHandler.class).in(Singleton.class);
    }
}
