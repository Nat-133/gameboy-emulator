package org.gameboy.components;

import org.gameboy.common.ByteRegister;
import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import static org.gameboy.utils.BitUtilities.get_bit;

public class Timer {
    private enum ReloadPhase {
        NORMAL,
        RELOAD_PENDING,
        RELOADING
    }

    private final InternalTimerCounter internalCounter;
    private final ByteRegister timaRegister;
    private final ByteRegister underlyingTima;
    private final ByteRegister timerModulo;
    private final ByteRegister timerControl;
    private final InterruptController interruptController;

    private boolean wasTimerBitHigh = false;
    private ReloadPhase reloadPhase = ReloadPhase.NORMAL;
    private boolean wasOverflowCancelled = false;
    private byte capturedTmaValue = 0;

    public ByteRegister getTimaRegister() {
        return timaRegister;
    }

    public Timer(InternalTimerCounter internalCounter,
                 ByteRegister tima,
                 ByteRegister tma,
                 TacRegister tac,
                 InterruptController interruptController) {
        this.internalCounter = internalCounter;
        this.underlyingTima = tima;
        this.timaRegister = createTimaWrapper(tima);
        this.timerModulo = tma;
        this.timerControl = tac;
        this.interruptController = interruptController;

        internalCounter.setResetListener(this::checkForFallingEdge);
        tac.setWriteListener(this::checkForFallingEdge);
    }

    private ByteRegister createTimaWrapper(ByteRegister tima) {
        return new ByteRegister() {
            @Override
            public byte read() {
                return tima.read();
            }

            @Override
            public void write(byte newValue) {
                switch (reloadPhase) {
                    case RELOAD_PENDING -> {
                        wasOverflowCancelled = true;
                        tima.write(newValue);
                    }
                    case RELOADING -> {}
                    case NORMAL -> tima.write(newValue);
                }
            }
        };
    }

    public void mCycle() {
        transitionReloadPhase();
        applyReloadIfActive();

        for (int i = 0; i < 4; i++) {
            internalCounter.tCycle();
            checkForFallingEdge();
        }
    }

    private void transitionReloadPhase() {
        switch (reloadPhase) {
            case RELOAD_PENDING -> {
                if (wasOverflowCancelled) {
                    reloadPhase = ReloadPhase.NORMAL;
                    wasOverflowCancelled = false;
                } else {
                    startReloading();
                }
            }
            case RELOADING -> reloadPhase = ReloadPhase.NORMAL;
            case NORMAL -> {}
        }
    }

    private void startReloading() {
        reloadPhase = ReloadPhase.RELOADING;
        capturedTmaValue = timerModulo.read();
        interruptController.setInterrupt(Interrupt.TIMER);
    }

    private void applyReloadIfActive() {
        if (reloadPhase == ReloadPhase.RELOADING) {
            underlyingTima.write(capturedTmaValue);
        }
    }

    private void checkForFallingEdge() {
        boolean isTimerBitHigh = isTimerBitHigh();

        if (wasTimerBitHigh && !isTimerBitHigh) {
            incrementTima();
        }

        wasTimerBitHigh = isTimerBitHigh;
    }

    private boolean isTimerBitHigh() {
        if (!isTimerEnabled()) {
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

    private void incrementTima() {
        underlyingTima.write((byte) (underlyingTima.read() + 1));

        if (underlyingTima.read() == 0) {
            reloadPhase = ReloadPhase.RELOAD_PENDING;
            wasOverflowCancelled = false;
        }
    }

    private boolean isTimerEnabled() {
        return get_bit(timerControl.read(), 2);
    }
}
