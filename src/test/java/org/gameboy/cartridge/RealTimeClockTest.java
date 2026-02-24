package org.gameboy.cartridge;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.assertj.core.api.Assertions.assertThat;

public class RealTimeClockTest {

    private static Supplier<Instant> fixedClock(AtomicReference<Instant> time) {
        return time::get;
    }

    // --- Latch Mechanism ---

    @Test
    public void givenNewRtc_whenReadWithoutLatch_thenReturnsZero() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 0x00);
        assertThatHex(rtc.read(0x09)).isEqualTo((byte) 0x00);
        assertThatHex(rtc.read(0x0A)).isEqualTo((byte) 0x00);
        assertThatHex(rtc.read(0x0B)).isEqualTo((byte) 0x00);
        assertThatHex(rtc.read(0x0C)).isEqualTo((byte) 0x00);
    }

    @Test
    public void givenTimeAdvanced_whenLatchedWith0x00Then0x01_thenReadsLatchedValues() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        // Advance 5 seconds
        time.set(Instant.EPOCH.plusSeconds(5));
        rtc.tick();

        // Latch
        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 5); // seconds
    }

    @Test
    public void givenLatchedValues_whenTimeAdvancesFurther_thenLatchedValuesUnchanged() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(5));
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        // Advance more time
        time.set(Instant.EPOCH.plusSeconds(30));
        rtc.tick();

        // Latched values should still show 5 seconds
        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 5);
    }

    @Test
    public void givenNoLeading0x00_whenWrite0x01_thenLatchDoesNotTrigger() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(10));
        rtc.tick();

        // Write 0x01 without preceding 0x00
        rtc.writeLatch((byte) 0x01);

        // Latched values should still be initial (0)
        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 0);
    }

    @Test
    public void givenLatchAlreadyDone_whenWrite0x01Again_thenDoesNotRelatch() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(5));
        rtc.tick();

        // First latch
        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        // Advance time
        time.set(Instant.EPOCH.plusSeconds(30));
        rtc.tick();

        // Second 0x01 without preceding 0x00 — should NOT relatch
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 5);
    }

    @Test
    public void givenLatchDone_whenNewSequence0x00Then0x01_thenRelatches() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(5));
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        // Advance time
        time.set(Instant.EPOCH.plusSeconds(30));
        rtc.tick();

        // New latch sequence
        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 30);
    }

    // --- Time Counting ---

    @Test
    public void givenTimeAdvanced65Seconds_thenMinuteAndSecondsCorrect() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(65)); // 1 min 5 sec
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 5);  // seconds
        assertThatHex(rtc.read(0x09)).isEqualTo((byte) 1);  // minutes
    }

    @Test
    public void givenTimeAdvanced3661Seconds_thenHoursMinutesSecondsCorrect() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(3661)); // 1h 1m 1s
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 1);  // seconds
        assertThatHex(rtc.read(0x09)).isEqualTo((byte) 1);  // minutes
        assertThatHex(rtc.read(0x0A)).isEqualTo((byte) 1);  // hours
    }

    @Test
    public void givenTimeAdvanced1Day_thenDayCounterIs1() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(86400)); // 1 day
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0B)).isEqualTo((byte) 1);  // DL
        assertThatHex(rtc.read(0x0C)).isEqualTo((byte) 0);  // DH
    }

    @Test
    public void givenTimeAdvanced256Days_thenDayLowOverflowsToDayHigh() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(256L * 86400)); // 256 days
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0B)).isEqualTo((byte) 0x00);  // DL (256 & 0xFF = 0)
        assertThatHex(rtc.read(0x0C)).isEqualTo((byte) 0x01);  // DH bit 0 = 1 (day MSB)
    }

    @Test
    public void givenTimeAdvanced511Days_thenDayCounterIs511() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(511L * 86400)); // 511 days
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0B)).isEqualTo((byte) 0xFF);  // DL = 255
        assertThatHex(rtc.read(0x0C)).isEqualTo((byte) 0x01);  // DH bit 0 = 1
    }

    @Test
    public void givenTimeAdvanced512Days_thenDayCounterWrapsAndCarrySet() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(512L * 86400)); // 512 days
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0B)).isEqualTo((byte) 0x00);  // DL wraps to 0
        assertThatHex(rtc.read(0x0C)).isEqualTo((byte) 0x80);  // DH: carry bit (bit 7) set, day MSB = 0
    }

    // --- Halt Flag ---

    @Test
    public void givenHaltFlagSet_whenTimeAdvances_thenCounterDoesNotIncrement() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        // Write halt flag (bit 6 of DH register)
        rtc.write(0x0C, (byte) 0x40);

        time.set(Instant.EPOCH.plusSeconds(10));
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 0); // seconds should not advance
    }

    @Test
    public void givenHaltFlagCleared_thenCounterResumesFromWhereItStopped() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        // Advance 5 seconds
        time.set(Instant.EPOCH.plusSeconds(5));
        rtc.tick();

        // Halt
        rtc.write(0x0C, (byte) 0x40);

        // Advance 100 seconds while halted
        time.set(Instant.EPOCH.plusSeconds(105));
        rtc.tick();

        // Unhalt
        rtc.write(0x0C, (byte) 0x00);

        // Advance 3 more seconds
        time.set(Instant.EPOCH.plusSeconds(108));
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        // Should be 5 + 3 = 8 seconds (100 halted seconds not counted)
        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 8);
    }

    // --- Writing RTC Registers ---

    @Test
    public void givenWriteToSeconds_thenLiveValueUpdated() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        rtc.write(0x08, (byte) 30);

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 30);
    }

    @Test
    public void givenWriteToMinutes_thenLiveValueUpdated() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        rtc.write(0x09, (byte) 45);

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x09)).isEqualTo((byte) 45);
    }

    @Test
    public void givenWriteToHours_thenLiveValueUpdated() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        rtc.write(0x0A, (byte) 12);

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0A)).isEqualTo((byte) 12);
    }

    @Test
    public void givenWriteToDayLow_thenLiveValueUpdated() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        rtc.write(0x0B, (byte) 0xAB);

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0B)).isEqualTo((byte) 0xAB);
    }

    @Test
    public void givenWriteToDayHigh_thenLiveValueUpdated() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        rtc.write(0x0C, (byte) 0xC1); // carry + halt + day MSB

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        assertThatHex(rtc.read(0x0C)).isEqualTo((byte) 0xC1);
    }

    @Test
    public void givenCarryFlagSet_thenRemainsSetUntilCleared() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        // Overflow past 512 days
        time.set(Instant.EPOCH.plusSeconds(512L * 86400));
        rtc.tick();

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);
        assertThat(rtc.read(0x0C) & 0x80).isNotZero(); // carry set

        // Clear carry by writing DH without carry bit
        rtc.write(0x0C, (byte) 0x00);

        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);
        assertThatHex((byte) (rtc.read(0x0C) & 0x80)).isEqualTo((byte) 0x00); // carry cleared
    }

    // --- Write to live, read from latch ---

    @Test
    public void givenWriteToLiveRegister_thenLatchStillReturnsOldValues() {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.EPOCH);
        RealTimeClock rtc = new RealTimeClock(fixedClock(time));

        time.set(Instant.EPOCH.plusSeconds(5));
        rtc.tick();

        // Latch (captures 5 seconds)
        rtc.writeLatch((byte) 0x00);
        rtc.writeLatch((byte) 0x01);

        // Write to live seconds register
        rtc.write(0x08, (byte) 30);

        // Latched value should still be 5
        assertThatHex(rtc.read(0x08)).isEqualTo((byte) 5);
    }
}
