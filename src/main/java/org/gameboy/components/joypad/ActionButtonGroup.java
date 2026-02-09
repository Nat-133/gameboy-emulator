package org.gameboy.components.joypad;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.components.joypad.annotations.*;

@Singleton
public class ActionButtonGroup extends ButtonGroup {

    @Inject
    public ActionButtonGroup(@ButtonA Button a,
                             @ButtonB Button b,
                             @ButtonSelect Button select,
                             @ButtonStart Button start) {
        super(a, b, select, start);
    }
}
