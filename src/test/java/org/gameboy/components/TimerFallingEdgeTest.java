package org.gameboy.components;

import org.gameboy.common.ByteRegister;
import org.gameboy.common.IntBackedRegister;
import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.mockito.Mockito.*;

class TimerFallingEdgeTest {

    private ByteRegister divRegister;
    private ByteRegister timaRegister;
    private ByteRegister tmaRegister;
    private ByteRegister tacRegister;
    private InterruptController interruptController;
    private Timer timer;

    @BeforeEach
    void setUp() {
        InternalTimerCounter internalCounter = new InternalTimerCounter();
        divRegister = new DividerRegister(internalCounter);
        timaRegister = new IntBackedRegister();
        tmaRegister = new IntBackedRegister();
        TacRegister tacReg = new TacRegister();
        tacRegister = tacReg;
        interruptController = mock(InterruptController.class);

        timer = new Timer(internalCounter, timaRegister, tmaRegister, tacReg, interruptController);
    }

    @Test
    void divRegister_shouldBeUpperBitsOfInternalCounter() {
        assertThatHex(divRegister.read()).isEqualTo((byte) 0x00);

        // 64 M-cycles = 256 T-cycles, counter = 0x0100, DIV (upper 8 bits) = 0x01
        for (int i = 0; i < 64; i++) {
            timer.mCycle();
        }
        assertThatHex(divRegister.read()).isEqualTo((byte) 0x01);
    }

    @Test
    void writingToDIV_shouldResetEntireInternalCounter() {
        // 320 M-cycles = 1280 T-cycles, counter = 0x0500, DIV = 0x05
        for (int i = 0; i < 320; i++) {
            timer.mCycle();
        }
        assertThatHex(divRegister.read()).isEqualTo((byte) 0x05);

        divRegister.write((byte) 0xFF);

        assertThatHex(divRegister.read()).isEqualTo((byte) 0x00);
    }

    @Test
    void timer4096Hz_shouldIncrementTimaEvery256MCycles() {
        // 4096 Hz mode (TAC=00) monitors bit 9, period = 256 M-cycles
        tacRegister.write((byte) 0b00000100);
        timaRegister.write((byte) 0x00);

        for (int i = 0; i < 256; i++) {
            timer.mCycle();
        }

        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x01);
    }

    @Test
    void timer262144Hz_shouldIncrementTimaEvery4MCycles() {
        // 262144 Hz mode (TAC=01) monitors bit 3, period = 4 M-cycles
        tacRegister.write((byte) 0b00000101);
        timaRegister.write((byte) 0x00);

        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x01);
    }

    @Test
    void timer65536Hz_shouldIncrementTimaEvery16MCycles() {
        // 65536 Hz mode (TAC=10) monitors bit 5, period = 16 M-cycles
        tacRegister.write((byte) 0b00000110);
        timaRegister.write((byte) 0x00);

        for (int i = 0; i < 16; i++) {
            timer.mCycle();
        }

        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x01);
    }

    @Test
    void timer16384Hz_shouldIncrementTimaEvery64MCycles() {
        // 16384 Hz mode (TAC=11) monitors bit 7, period = 64 M-cycles
        tacRegister.write((byte) 0b00000111);
        timaRegister.write((byte) 0x00);

        for (int i = 0; i < 64; i++) {
            timer.mCycle();
        }

        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x01);
    }

    @Test
    void writingToDIV_whenMonitoredBitIsHigh_shouldTriggerFallingEdgeAndIncrementTima() {
        // 4096 Hz mode monitors bit 9
        tacRegister.write((byte) 0b00000100);
        timaRegister.write((byte) 0x00);

        // 128 M-cycles = 512 T-cycles, bit 9 = 1
        for (int i = 0; i < 128; i++) {
            timer.mCycle();
        }

        byte timaBeforeWrite = timaRegister.read();

        // Write to DIV causes bit 9 to go 1→0 (falling edge)
        divRegister.write((byte) 0x00);

        assertThatHex(timaRegister.read()).isEqualTo((byte) (timaBeforeWrite + 1));
    }

    @Test
    void writingToDIV_whenMonitoredBitIsLow_shouldNotIncrementTima() {
        // 4096 Hz mode monitors bit 9
        tacRegister.write((byte) 0b00000100);
        timaRegister.write((byte) 0x00);

        // 64 M-cycles = 256 T-cycles, bit 9 = 0
        for (int i = 0; i < 64; i++) {
            timer.mCycle();
        }

        byte timaBeforeWrite = timaRegister.read();

        // Write to DIV, bit 9 stays 0→0 (no edge)
        divRegister.write((byte) 0x00);

        assertThatHex(timaRegister.read()).isEqualTo(timaBeforeWrite);
    }

    @Test
    void timaOverflow_shouldResetToTmaAndTriggerInterrupt() {
        // 262144 Hz mode (fastest), period = 4 M-cycles
        tacRegister.write((byte) 0b00000101);
        timaRegister.write((byte) 0xFF);
        tmaRegister.write((byte) 0x42);

        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x42);
        verify(interruptController).setInterrupt(Interrupt.TIMER);
    }

    @Test
    void divWrite_shouldPreventTimaIncrementByResettingCounter() {
        // 4096 Hz mode, period = 256 M-cycles
        tacRegister.write((byte) 0b00000100);
        timaRegister.write((byte) 0xFF);

        for (int i = 0; i < 500; i++) {
            timer.mCycle();
            if (i % 10 == 0) {
                divRegister.write((byte) 0x00);
            }
        }

        byte finalTima = timaRegister.read();

        assertThat(finalTima).isNotEqualTo((byte) 0x00);
        verify(interruptController, never()).setInterrupt(Interrupt.TIMER);
    }
}
