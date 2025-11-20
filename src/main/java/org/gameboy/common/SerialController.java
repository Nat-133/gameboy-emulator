package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.components.InternalTimerCounter;

@Singleton
public class SerialController {
    private static final int SERIAL_CLOCK_BIT = 7;

    private byte serialData = 0;
    private byte serialControl = 0;
    private boolean transferPending = false;
    private boolean serialMasterClock = true;
    private int serialBitCount = 0;

    private final InterruptController interruptController;
    private final StringBuilder outputBuffer = new StringBuilder();

    @Inject
    public SerialController(InterruptController interruptController,
                           InternalTimerCounter internalTimerCounter) {
        this.interruptController = interruptController;
        internalTimerCounter.onFallingEdge(SERIAL_CLOCK_BIT, this::onSerialClockEdge);
    }

    private void onSerialClockEdge() {
        serialMasterClock = !serialMasterClock;

        // Shift on every other edge (when master clock goes false)
        if (!serialMasterClock && transferPending) {
            serialBitCount++;
            if (serialBitCount == 8) {
                completeTransfer();
            }
        }
    }

    public byte readSerialData() {
        return serialData;
    }

    public void writeSerialData(byte value) {
        serialData = value;
    }

    public byte readSerialControl() {
        return serialControl;
    }

    public void writeSerialControl(byte value) {
        serialControl = value;

        if ((value & 0x81) == 0x81) {
            // Print debug output immediately (for Blargg tests)
            int unsignedByte = serialData & 0xFF;

            if ((unsignedByte >= 32 && unsignedByte <= 126) ||
                unsignedByte == '\n' || unsignedByte == '\r' || unsignedByte == '\t') {
                char character = (char) unsignedByte;
                System.out.print(character);
                outputBuffer.append(character);
            } else {
                String hexString = String.format("[0x%02X]", unsignedByte);
                System.out.print(hexString);
                outputBuffer.append(hexString);
            }

            // Start transfer - will complete at clock boundary
            transferPending = true;
            serialBitCount = 0;
        }
    }

    private void completeTransfer() {
        serialData = (byte) 0xFF;
        serialControl = (byte) (serialControl & 0x7F);  // Clear bit 7
        interruptController.setInterrupt(Interrupt.SERIAL);
        transferPending = false;
    }

    public String getOutput() {
        return outputBuffer.toString();
    }

    public void clearOutput() {
        outputBuffer.setLength(0);
    }

    // Keep mCycle() for interface compatibility, but timing is handled by callback
    public void mCycle() {
        // Timing now handled by InternalTimerCounter callback
    }
}
