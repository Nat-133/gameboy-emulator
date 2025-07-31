package org.gameboy.components;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.common.ByteRegister;
import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.gameboy.common.annotations.Div;
import org.gameboy.common.annotations.Tac;
import org.gameboy.common.annotations.Tima;
import org.gameboy.common.annotations.Tma;
import org.gameboy.utils.BitUtilities;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

@Singleton
public class Timer {
    private static final int M_CYCLES_PER_DIV_INCREMENT = 64;

    private final ByteRegister dividerRegister;  // DIV
    private final ByteRegister timerCounterRegister;  // TIMA
    private final ByteRegister timerModulo;  // TMA
    private final ByteRegister timerControl;  // TAC
    private final InterruptController interruptController;

    private int internalCounter;

    @Inject
    public Timer(@Tima ByteRegister tima,
                 @Div ByteRegister div,
                 @Tma ByteRegister tma,
                 @Tac ByteRegister tac,
                 InterruptController interruptController) {
        this.timerCounterRegister = tima;
        this.dividerRegister = div;
        this.timerModulo = tma;
        this.timerControl = tac;
        this.interruptController = interruptController;

        this.internalCounter = 0;
    }

    public void mCycle() {
        internalCounter = (internalCounter + 1) % 256;
        
        if (internalCounter % M_CYCLES_PER_DIV_INCREMENT == 0) {
            dividerRegister.write((byte) (dividerRegister.read() + 1));
        }
        
        if (timerEnabled() && internalCounter % mCyclesPerTimerIncrement() == 0) {
            handleTimerIncrement();
        }
    }

    private void handleTimerIncrement() {
        timerCounterRegister.write((byte) (timerCounterRegister.read() + 1));

        if (timerCounterRegister.read() == 0) {
            timerCounterRegister.write(timerModulo.read());
            interruptController.setInterrupt(Interrupt.TIMER);
        }
    }

    private int mCyclesPerTimerIncrement() {
        return switch(TwoBitValue.from(timerControl.read())) {
            case b00 -> 256;
            case b01 -> 4;
            case b10 -> 16;
            case b11 -> 64;
        };
    }

    private boolean timerEnabled() {
        return BitUtilities.get_bit(timerControl.read(), 2);
    }
}
