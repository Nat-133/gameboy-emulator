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
        ByteRegister unwrappedTima = new IntBackedRegister();
        tmaRegister = new IntBackedRegister();
        TacRegister tacReg = new TacRegister();
        tacRegister = tacReg;
        interruptController = mock(InterruptController.class);

        timer = new Timer(internalCounter, unwrappedTima, tmaRegister, tacReg, interruptController);
        // Use the wrapped TIMA register that the Timer exposes
        timaRegister = timer.getTimaRegister();
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

        timer.mCycle(); // Cycle B
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

    @Test
    void timaOverflow_shouldStayAtZeroForOneMCycleBeforeReloading() {
        // 262144 Hz mode (fastest), period = 4 M-cycles
        tacRegister.write((byte) 0b00000101);
        timaRegister.write((byte) 0xFF);
        tmaRegister.write((byte) 0x42);

        // Run 4 M-cycles to cause overflow: TIMA goes FF -> 00
        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        // TIMA should be 0x00 immediately after overflow (Cycle A - rest of this M-cycle)
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x00);

        // During Cycle B (next M-cycle), TIMA is reloaded with TMA value
        timer.mCycle();
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x42);
        verify(interruptController).setInterrupt(Interrupt.TIMER);
    }

    @Test
    void writingToTimaDuringCycleA_shouldCancelOverflow() {
        // 262144 Hz mode (fastest), period = 4 M-cycles
        tacRegister.write((byte) 0b00000101);
        timaRegister.write((byte) 0xFF);
        tmaRegister.write((byte) 0x42);

        // Run 4 M-cycles to cause overflow: TIMA goes FF -> 00
        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        // TIMA should be 0x00 during Cycle A (first M-cycle after overflow)
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x00);

        // Write to TIMA during Cycle A should cancel overflow completely
        timaRegister.write((byte) 0x99);

        // Run 1 more M-cycle to complete the sequence
        timer.mCycle();

        // TIMA should still be 0x99 (overflow was cancelled, no reload)
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x99);
        // Interrupt should NOT fire (overflow was cancelled)
        verify(interruptController, never()).setInterrupt(Interrupt.TIMER);
    }

    @Test
    void writingToTmaDuringCycleB_shouldAffectReloadValue() {
        // 262144 Hz mode (fastest), period = 4 M-cycles
        tacRegister.write((byte) 0b00000101);
        timaRegister.write((byte) 0xFF);
        tmaRegister.write((byte) 0x42);

        // Run 4 M-cycles to cause overflow: TIMA goes FF -> 00
        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        // TIMA should be 0x00 during Cycle A (rest of current M-cycle)
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x00);

        // Write to TMA, then enter Cycle B where reload happens
        tmaRegister.write((byte) 0x88);

        // Run Cycle B - TIMA should be loaded with new TMA value
        timer.mCycle();
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x88);
        verify(interruptController).setInterrupt(Interrupt.TIMER);
    }

    @Test
    void tmaWriteBeforeCycleB_shouldAffectReload() {
        // TMA is captured at START of Cycle B
        // Writing BEFORE Cycle B should affect reload
        tacRegister.write((byte) 0b00000101); // 262144 Hz, period = 4 M-cycles
        timaRegister.write((byte) 0xFF);
        tmaRegister.write((byte) 0x42);

        // Run to overflow
        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        // Now in Cycle A, TIMA = 0x00
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x00);

        // Write TMA BEFORE entering Cycle B
        tmaRegister.write((byte) 0x99);

        // Enter Cycle B - should capture new TMA value (0x99)
        timer.mCycle();

        // TIMA should have new TMA value
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x99);
        verify(interruptController).setInterrupt(Interrupt.TIMER);
    }

    @Test
    void tmaWriteAfterCycleB_shouldNotAffectReload() {
        // TMA is captured at START of Cycle B
        // Writing AFTER Cycle B starts shouldn't affect THIS reload
        tacRegister.write((byte) 0b00000101); // 262144 Hz, period = 4 M-cycles
        timaRegister.write((byte) 0xFF);
        tmaRegister.write((byte) 0x42);

        // Run to overflow
        for (int i = 0; i < 4; i++) {
            timer.mCycle();
        }

        // Now in Cycle A, enter Cycle B (TMA is captured as 0x42)
        timer.mCycle();

        // Now IN Cycle B, write TMA (too late to affect THIS reload)
        tmaRegister.write((byte) 0x99);

        // TIMA should have original TMA value (0x42), not new one
        assertThatHex(timaRegister.read()).isEqualTo((byte) 0x42);
        verify(interruptController).setInterrupt(Interrupt.TIMER);
    }

    @Test
    void tacWrite_shouldDetectFallingEdgeWhenDisabling() {
        // When timer is disabled via TAC write, if monitored bit is high,
        // it causes a falling edge that increments TIMA
        tacRegister.write((byte) 0b00000100); // Enable 4096 Hz (bit 9 monitored)
        timaRegister.write((byte) 0x00);

        // Run timer until monitored bit (bit 9) is high
        // Bit 9 cycles every 1024 T-cycles = 256 M-cycles
        // It's high for half that time = 128 M-cycles
        for (int i = 0; i < 128; i++) {
            timer.mCycle();
        }

        byte timaBefore = timaRegister.read();

        // Disable timer - this should cause falling edge if bit 9 is high
        tacRegister.write((byte) 0b00000000);

        byte timaAfter = timaRegister.read();

        // TIMA should have incremented due to glitchy increment
        // (We ran 128 M-cycles, so TIMA incremented once normally,
        //  plus should get one glitchy increment from disabling)
        assertThat(timaAfter).isGreaterThan(timaBefore);
    }

    @Test
    void rapidTimerToggle_shouldCauseGlitchyIncrements() {
        // Replicate mooneye rapid_toggle test
        // Rapidly toggling timer on/off causes falling edges that increment TIMA
        tacRegister.write((byte) 0b00000100); // Start at 4096 Hz (bit 9, period 256 M-cycles)
        timaRegister.write((byte) 0xFE);

        // Run to get bit 9 into high phase (M-cycle 128-255)
        for (int i = 0; i < 128; i++) {
            timer.mCycle();
        }

        int glitchyIncrements = 0;

        // Now toggle repeatedly while bit 9 is high
        for (int i = 0; i < 64; i++) {  // Toggle 64 times during high phase
            byte before = timaRegister.read();
            tacRegister.write((byte) 0b00000100); // Enable
            tacRegister.write((byte) 0b00000000); // Disable (should cause glitchy increment)
            byte after = timaRegister.read();

            if (after > before) {
                glitchyIncrements++;
            }

            timer.mCycle();
        }

        // We should have gotten many glitchy increments
        // Each toggle when bit is high should cause an increment
        assertThat(glitchyIncrements).isGreaterThan(50);
    }
}
