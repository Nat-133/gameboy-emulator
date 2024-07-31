package org.gameboy;

import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTest {
    @Test
    public void givenMemorySave_whenMemoryLoad_thenCorrect() {
        Memory memory = new Memory();
        for (int i = 0x0000; i < 0xFFFF; i++) {
            memory.write((short) i, (byte) i);
        }

        for (int i = 0x0000; i < 0xFFFF; i++) {
            short val = memory.read((short) i);
            assertEquals((byte) i, (byte) val);

            byte expectedValue = (byte) (i==0xFFFE ? 0 : i+1);
            assertEquals(expectedValue, BitUtilities.upper_byte(val));
        }
    }
}