package org.gameboy.components.joypad;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.gameboy.common.ByteRegister;
import org.gameboy.components.joypad.annotations.*;

public class JoypadModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ByteRegister.class).annotatedWith(org.gameboy.common.annotations.Joypad.class).to(JoypadController.class).in(Singleton.class);

        // D-pad buttons
        Button rightButton = new Button();
        bind(Button.class).annotatedWith(ButtonRight.class).toInstance(rightButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonRight.class).toInstance(new MultiSourceButton(rightButton));

        Button leftButton = new Button();
        bind(Button.class).annotatedWith(ButtonLeft.class).toInstance(leftButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonLeft.class).toInstance(new MultiSourceButton(leftButton));

        Button upButton = new Button();
        bind(Button.class).annotatedWith(ButtonUp.class).toInstance(upButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonUp.class).toInstance(new MultiSourceButton(upButton));

        Button downButton = new Button();
        bind(Button.class).annotatedWith(ButtonDown.class).toInstance(downButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonDown.class).toInstance(new MultiSourceButton(downButton));

        // Action buttons
        Button aButton = new Button();
        bind(Button.class).annotatedWith(ButtonA.class).toInstance(aButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonA.class).toInstance(new MultiSourceButton(aButton));

        Button bButton = new Button();
        bind(Button.class).annotatedWith(ButtonB.class).toInstance(bButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonB.class).toInstance(new MultiSourceButton(bButton));

        Button selectButton = new Button();
        bind(Button.class).annotatedWith(ButtonSelect.class).toInstance(selectButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonSelect.class).toInstance(new MultiSourceButton(selectButton));

        Button startButton = new Button();
        bind(Button.class).annotatedWith(ButtonStart.class).toInstance(startButton);
        bind(MultiSourceButton.class).annotatedWith(ButtonStart.class).toInstance(new MultiSourceButton(startButton));
    }
}
