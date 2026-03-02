package org.gameboy.components;

import com.google.inject.Inject;
import org.gameboy.common.ByteRegister;

public class TimaRegister implements ByteRegister {
    private final Timer timer;

    @Inject
    public TimaRegister(Timer timer) {
        this.timer = timer;
    }

    @Override
    public byte read() {
        return timer.readTima();
    }

    @Override
    public void write(byte newValue) {
        timer.writeTima(newValue);
    }
}
