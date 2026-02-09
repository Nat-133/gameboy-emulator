package org.gameboy.components.joypad;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.components.joypad.annotations.*;

@Singleton
public class DpadButtonGroup extends ButtonGroup {

    @Inject
    public DpadButtonGroup(@ButtonRight Button right,
                           @ButtonLeft Button left,
                           @ButtonUp Button up,
                           @ButtonDown Button down) {
        super(right, left, up, down);
    }
}
