package org.gameboy.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterruptFlagsRegisterTest {

    @Test
    void whenWritingZero_shouldReadAsE0() {
        ByteRegister ifRegister = new InterruptFlagsRegister();

        ifRegister.write((byte) 0x00);

        byte result = ifRegister.read();
        assertThat(result).isEqualTo((byte) 0xE0);
    }

    @Test
    void whenWritingSerialInterrupt_shouldReadWithUpperBitsSet() {
        ByteRegister ifRegister = new InterruptFlagsRegister();

        ifRegister.write((byte) 0x08);

        byte result = ifRegister.read();
        assertThat(result).isEqualTo((byte) 0xE8);
    }

    @Test
    void whenWritingAllInterruptBits_shouldReadWithUpperBitsSet() {
        ByteRegister ifRegister = new InterruptFlagsRegister();

        ifRegister.write((byte) 0x1F);

        byte result = ifRegister.read();
        assertThat(result).isEqualTo((byte) 0xFF);
    }

    @Test
    void whenWritingVBlankInterrupt_shouldReadWithUpperBitsSet() {
        ByteRegister ifRegister = new InterruptFlagsRegister();

        ifRegister.write((byte) 0x01);

        byte result = ifRegister.read();
        assertThat(result).isEqualTo((byte) 0xE1);
    }

    @Test
    void whenClearingSpecificBit_shouldReadWithUpperBitsSet() {
        ByteRegister ifRegister = new InterruptFlagsRegister();

        ifRegister.write((byte) 0x08);
        assertThat(ifRegister.read()).isEqualTo((byte) 0xE8);

        ifRegister.write((byte) 0x00);

        byte result = ifRegister.read();
        assertThat(result).isEqualTo((byte) 0xE0);
    }

    @Test
    void whenWritingUpperBits_shouldIgnoreThem() {
        ByteRegister ifRegister = new InterruptFlagsRegister();

        ifRegister.write((byte) 0x08);
        assertThat(ifRegister.read()).isEqualTo((byte) 0xE8);

        ifRegister.write((byte) 0xFF);
        assertThat(ifRegister.read()).isEqualTo((byte) 0xFF);

        ifRegister.write((byte) 0xE0);
        assertThat(ifRegister.read()).isEqualTo((byte) 0xE0);
    }
}
