package org.gameboy.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryTest {
    @Test
    public void givenMemorySave_whenMemoryLoad_thenCorrect() {
        Memory memory = new BasicMemory();
        for (int i = 0x0000; i < 0xFFFF; i++) {
            memory.write((short) i, (byte) i);
        }

        for (int i = 0x0000; i < 0xFFFF; i++) {
            byte val = memory.read((short) i);
            assertEquals((byte) i, val);
        }
    }
}