package org.gameboy.common;

import org.gameboy.components.InternalTimerCounter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerialControllerTest {
    private static final int SERIAL_INTERRUPT_BIT = Interrupt.SERIAL.index();

    private SerialController serialController;
    private ByteRegister interruptFlagsRegister;
    private InternalTimerCounter internalTimerCounter;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        interruptFlagsRegister = new IntBackedRegister();
        InterruptController interruptController = new InterruptController(interruptFlagsRegister);
        internalTimerCounter = new InternalTimerCounter(0);
        serialController = new SerialController(interruptController, internalTimerCounter);

        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    private boolean isSerialInterruptSet() {
        return (interruptFlagsRegister.read() & (1 << SERIAL_INTERRUPT_BIT)) != 0;
    }

    private void clearSerialInterrupt() {
        byte flags = interruptFlagsRegister.read();
        interruptFlagsRegister.write((byte) (flags & ~(1 << SERIAL_INTERRUPT_BIT)));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void testSerialDataReadWrite() {
        byte testValue = (byte) 0x42;

        serialController.writeSerialData(testValue);
        assertThatHex(serialController.readSerialData()).isEqualTo(testValue);
    }

    @Test
    public void testSerialControlReadWrite() {
        byte testValue = (byte) 0x7F;

        serialController.writeSerialControl(testValue);
        assertThatHex(serialController.readSerialControl()).isEqualTo(testValue);
    }

    @Test
    public void testSerialTransferWithInterrupt() {
        byte testChar = (byte) 'A';
        serialController.writeSerialData(testChar);
        serialController.writeSerialControl((byte) 0x81);

        assertEquals("A", outputStream.toString());
        assertEquals("A", serialController.getOutput());

        // Run 4096 T-cycles to complete the transfer (1024 M-cycles)
        for (int i = 0; i < 4096; i++) {
            internalTimerCounter.tCycle();
        }

        assertThat(isSerialInterruptSet()).isTrue();
        assertThatHex(serialController.readSerialData()).isEqualTo((byte) 0xFF);
        assertThatHex(serialController.readSerialControl()).isEqualTo((byte) 0x01);
    }

    @Test
    public void testNoTransferWithoutProperControl() {
        byte testChar = (byte) 'B';
        serialController.writeSerialData(testChar);

        // Only bit 0 set (internal clock), but not bit 7 (transfer enable)
        serialController.writeSerialControl((byte) 0x01);

        assertEquals("", outputStream.toString());
        assertThat(isSerialInterruptSet()).isFalse();
        assertThatHex(serialController.readSerialData()).isEqualTo(testChar);
    }

    @Test
    public void testMultipleCharacterOutput() {
        String expectedOutput = "Hello";
        int interruptCount = 0;

        for (char c : expectedOutput.toCharArray()) {
            clearSerialInterrupt();
            serialController.writeSerialData((byte) c);
            serialController.writeSerialControl((byte) 0x81);

            // Run 4096 T-cycles to complete each transfer
            for (int i = 0; i < 4096; i++) {
                internalTimerCounter.tCycle();
            }

            if (isSerialInterruptSet()) {
                interruptCount++;
            }
        }

        assertEquals(expectedOutput, outputStream.toString());
        assertEquals(expectedOutput, serialController.getOutput());
        assertThat(interruptCount).isEqualTo(5);
    }

    @Test
    public void testClearOutput() {
        serialController.writeSerialData((byte) 'X');
        serialController.writeSerialControl((byte) 0x81);

        // Run 4096 T-cycles to complete the transfer
        for (int i = 0; i < 4096; i++) {
            internalTimerCounter.tCycle();
        }

        assertEquals("X", serialController.getOutput());
        serialController.clearOutput();
        assertEquals("", serialController.getOutput());
    }

    @Test
    void serialTransfer_shouldNotCompleteImmediately() {
        serialController.writeSerialData((byte) 0x42);
        serialController.writeSerialControl((byte) 0x81);

        // Interrupt should NOT fire immediately
        assertThat(isSerialInterruptSet()).isFalse();

        // Serial data should still be 0x42 (not yet 0xFF)
        assertThatHex(serialController.readSerialData()).isEqualTo((byte) 0x42);

        // SC bit 7 should still be set (transfer in progress)
        assertThatHex(serialController.readSerialControl() & 0x80).isEqualTo(0x80);
    }

    @Test
    void serialTransfer_shouldCompleteAfter4096TCycles() {
        serialController.writeSerialData((byte) 0x55);
        serialController.writeSerialControl((byte) 0x81);

        // Run exactly 4096 T-cycles (8 bits × 512 T-cycles per bit)
        for (int i = 0; i < 4096; i++) {
            internalTimerCounter.tCycle();
        }

        assertThat(isSerialInterruptSet()).isTrue();
        assertThatHex(serialController.readSerialData()).isEqualTo((byte) 0xFF);
        assertThatHex(serialController.readSerialControl() & 0x80).isEqualTo(0x00);
    }

    @Test
    void serialTransfer_shouldNotCompleteBeforeFullTransfer() {
        serialController.writeSerialData((byte) 0x55);
        serialController.writeSerialControl((byte) 0x81);

        // Run less than full transfer (7 bits worth = 3584 T-cycles)
        for (int i = 0; i < 3584; i++) {
            internalTimerCounter.tCycle();
        }

        // Should NOT have completed yet
        assertThat(isSerialInterruptSet()).isFalse();
        assertThatHex(serialController.readSerialControl() & 0x80).isEqualTo(0x80);
    }

    @Test
    void serialTransfer_shouldNotFireWithoutStart() {
        // Run many cycles without starting transfer
        for (int i = 0; i < 10000; i++) {
            internalTimerCounter.tCycle();
        }

        assertThat(isSerialInterruptSet()).isFalse();
    }

    @Test
    void serialTransfer_shouldAlignToClockBoundary() {
        // Advance counter to a known position
        for (int i = 0; i < 1000; i++) {
            internalTimerCounter.tCycle();
        }

        serialController.writeSerialData((byte) 0xAA);
        serialController.writeSerialControl((byte) 0x81);

        // Transfer should complete based on global clock alignment
        // Run enough cycles to guarantee completion
        for (int i = 0; i < 4096; i++) {
            internalTimerCounter.tCycle();
        }

        assertThat(isSerialInterruptSet()).isTrue();
    }

    @Test
    void serialTransfer_shouldNotStartWithExternalClock() {
        // SC = 0x80 (transfer enable, but external clock - bit 0 = 0)
        serialController.writeSerialData((byte) 0x55);
        serialController.writeSerialControl((byte) 0x80);

        for (int i = 0; i < 10000; i++) {
            internalTimerCounter.tCycle();
        }

        assertThat(isSerialInterruptSet()).isFalse();
    }
}
