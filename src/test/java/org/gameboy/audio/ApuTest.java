package org.gameboy.audio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;

class ApuTest {
    private Apu apu;
    private ApuRegisters registers;
    private ConcurrentLinkedQueue<short[]> sampleQueue;

    @BeforeEach
    void setUp() {
        sampleQueue = new ConcurrentLinkedQueue<>();
        registers = new ApuRegisters();
        apu = new Apu(registers, sampleQueue);
    }

    @Test
    void powerOn_shouldBeEnabledByDefault() {
        registers.nr52.write((byte) 0x80);
        assertThat(apu.isPoweredOn()).isTrue();
    }

    @Test
    void powerOff_shouldDisableAllChannels() {
        registers.nr52.write((byte) 0x80);
        registers.nr12.write((byte) 0xF0);
        registers.nr14.write((byte) 0x80);
        registers.nr52.write((byte) 0x00);
        assertThat(apu.isPoweredOn()).isFalse();
        assertThatHex(registers.nr52.read()).isEqualTo((byte) 0x70);
    }

    @Test
    void tCycle_shouldProduceSamplesInQueue() {
        registers.nr52.write((byte) 0x80);
        registers.nr50.write((byte) 0x77);
        registers.nr51.write((byte) 0xFF);
        registers.nr11.write((byte) 0x80);
        registers.nr12.write((byte) 0xF0);
        registers.nr13.write((byte) 0x00);
        registers.nr14.write((byte) 0x80);
        for (int i = 0; i < 100_000; i++) apu.tCycle();
        assertThat(sampleQueue).isNotEmpty();
    }

    @Test
    void nr52StatusBits_shouldReflectChannelEnabled() {
        registers.nr52.write((byte) 0x80);
        registers.nr12.write((byte) 0xF0);
        registers.nr14.write((byte) 0x80);
        apu.tCycle();
        byte status = registers.nr52.read();
        assertThat(status & 0x01).isEqualTo(1);
    }

    @Test
    void registersIgnoredWhenPoweredOff() {
        registers.nr52.write((byte) 0x00);
        registers.nr12.write((byte) 0xF0);
        registers.nr14.write((byte) 0x80);
        byte status = registers.nr52.read();
        assertThat(status & 0x0F).isEqualTo(0);
    }
}
