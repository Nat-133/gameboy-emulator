package org.gameboy.components;

import org.gameboy.common.ByteRegister;
import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.utils.BitUtilities.get_bit;

public class Timer {
    private final InternalTimerCounter internalCounter;
    private final ByteRegister timerCounterRegister;
    private final ByteRegister timerModulo;
    private final ByteRegister timerControl;
    private final InterruptController interruptController;
    private boolean previousAndResult = false;

    public Timer(InternalTimerCounter internalCounter,
                 ByteRegister tima,
                 ByteRegister tma,
                 TacRegister tac,
                 InterruptController interruptController) {
        this.internalCounter = internalCounter;
        this.timerCounterRegister = tima;
        this.timerModulo = tma;
        this.timerControl = tac;
        this.interruptController = interruptController;

        internalCounter.setResetListener(this::checkForFallingEdge);
        tac.setWriteListener(this::onTacWrite);
    }

    public void mCycle() {
        for (int i = 0; i < 4; i++) {
            internalCounter.tCycle();
            checkForFallingEdge();
        }
    }

    public void onTacWrite() {
        checkForFallingEdge();
    }

    private void checkForFallingEdge() {
        boolean currentAndResult = getAndResult();

        if (previousAndResult && !currentAndResult) {
            handleTimerIncrement();
        }

        previousAndResult = currentAndResult;
    }

    private boolean getAndResult() {
        if (!timerEnabled()) {
            return false;
        }

        int monitoredBit = getMonitoredBit();
        int counterValue = internalCounter.getValue();
        return ((counterValue >> monitoredBit) & 1) == 1;
    }

    private int getMonitoredBit() {
        return switch(TwoBitValue.from(timerControl.read())) {
            case b00 -> 9;
            case b01 -> 3;
            case b10 -> 5;
            case b11 -> 7;
        };
    }

    private void handleTimerIncrement() {
        timerCounterRegister.write((byte) (timerCounterRegister.read() + 1));

        if (timerCounterRegister.read() == 0) {
            timerCounterRegister.write(timerModulo.read());
            interruptController.setInterrupt(Interrupt.TIMER);
        }
    }

    private boolean timerEnabled() {
        return get_bit(timerControl.read(), 2);
    }
}
