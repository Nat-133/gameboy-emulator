package org.gameboy.cartridge;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class RealTimeClock {
    private final Supplier<Instant> clock;

    // Live registers
    private int seconds;
    private int minutes;
    private int hours;
    private int dayLow;
    private int dayHigh; // bit 0 = day MSB, bit 6 = halt, bit 7 = carry

    // Latched registers
    private int latchedSeconds;
    private int latchedMinutes;
    private int latchedHours;
    private int latchedDayLow;
    private int latchedDayHigh;

    // Latch state: false = waiting for 0x00, true = received 0x00
    private boolean latchArmed;

    // Timing
    private Instant lastTickTime;
    private boolean halted;

    public RealTimeClock(Supplier<Instant> clock) {
        this.clock = clock;
        this.lastTickTime = clock.get();
    }

    public byte read(int register) {
        return (byte) switch (register) {
            case 0x08 -> latchedSeconds;
            case 0x09 -> latchedMinutes;
            case 0x0A -> latchedHours;
            case 0x0B -> latchedDayLow;
            case 0x0C -> latchedDayHigh;
            default -> 0xFF;
        };
    }

    public void write(int register, byte value) {
        int v = Byte.toUnsignedInt(value);
        switch (register) {
            case 0x08 -> seconds = v;
            case 0x09 -> minutes = v;
            case 0x0A -> hours = v;
            case 0x0B -> dayLow = v;
            case 0x0C -> {
                dayHigh = v;
                boolean wasHalted = halted;
                halted = (v & 0x40) != 0;
                if (wasHalted && !halted) {
                    // Resuming from halt — reset the tick baseline
                    lastTickTime = clock.get();
                }
                if (!wasHalted && halted) {
                    // Entering halt — consume elapsed time first
                    advanceLiveRegisters();
                }
            }
        }
    }

    public void writeLatch(byte value) {
        int v = Byte.toUnsignedInt(value);
        if (v == 0x00) {
            latchArmed = true;
        } else if (v == 0x01 && latchArmed) {
            latchArmed = false;
            latchedSeconds = seconds;
            latchedMinutes = minutes;
            latchedHours = hours;
            latchedDayLow = dayLow;
            latchedDayHigh = dayHigh;
        } else {
            latchArmed = false;
        }
    }

    public void tick() {
        if (!halted) {
            advanceLiveRegisters();
        }
    }

    private void advanceLiveRegisters() {
        Instant now = clock.get();
        long elapsedSeconds = Duration.between(lastTickTime, now).getSeconds();
        lastTickTime = now;

        if (elapsedSeconds <= 0) {
            return;
        }

        // Convert current state to total seconds
        int dayCount = dayLow | ((dayHigh & 0x01) << 8);
        boolean carry = (dayHigh & 0x80) != 0;

        long totalSeconds = seconds
                + (long) minutes * 60
                + (long) hours * 3600
                + (long) dayCount * 86400
                + elapsedSeconds;

        seconds = (int) (totalSeconds % 60);
        totalSeconds /= 60;

        minutes = (int) (totalSeconds % 60);
        totalSeconds /= 60;

        hours = (int) (totalSeconds % 24);
        totalSeconds /= 24;

        int newDayCount = (int) totalSeconds;

        if (newDayCount > 511) {
            carry = true;
            newDayCount %= 512;
        }

        dayLow = newDayCount & 0xFF;
        dayHigh = (dayHigh & 0x40) // preserve halt flag
                | (carry ? 0x80 : 0)
                | ((newDayCount >> 8) & 0x01);
    }
}
