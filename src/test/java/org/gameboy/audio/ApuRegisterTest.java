package org.gameboy.audio;

import org.junit.jupiter.api.Test;
import static org.gameboy.GameboyAssertions.assertThatHex;

class ApuRegisterTest {
    @Test
    void read_shouldMaskUnreadableBitsAsOnes() {
        ApuRegister reg = new ApuRegister(0x7F);
        reg.write((byte) 0x00);
        assertThatHex(reg.read()).isEqualTo((byte) 0x80);
    }

    @Test
    void read_writeOnlyRegister_shouldReturn0xFF() {
        ApuRegister reg = new ApuRegister(0x00);
        reg.write((byte) 0x42);
        assertThatHex(reg.read()).isEqualTo((byte) 0xFF);
    }

    @Test
    void read_fullyReadable_shouldReturnWrittenValue() {
        ApuRegister reg = new ApuRegister(0xFF);
        reg.write((byte) 0x42);
        assertThatHex(reg.read()).isEqualTo((byte) 0x42);
    }

    @Test
    void write_shouldCallWriteCallback() {
        boolean[] called = {false};
        ApuRegister reg = new ApuRegister(0xFF);
        reg.setWriteCallback(v -> called[0] = true);
        reg.write((byte) 0x01);
        org.assertj.core.api.Assertions.assertThat(called[0]).isTrue();
    }

    @Test
    void getRawValue_shouldReturnUnmaskedValue() {
        ApuRegister reg = new ApuRegister(0x00);
        reg.write((byte) 0x42);
        assertThatHex((byte) reg.getRawValue()).isEqualTo((byte) 0x42);
    }
}
