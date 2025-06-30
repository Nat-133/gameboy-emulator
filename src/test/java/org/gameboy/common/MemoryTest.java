package org.gameboy.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.common.MemoryMapConstants.IF_ADDRESS;
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

    @Test
    void givenMemoryListener_whenMemoryWriteToAddress_thenListenerNotified() {
        Memory memory = new BasicMemory();
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        memory.registerMemoryListener(IF_ADDRESS, () -> listenerCalled.set(true));

        memory.write(IF_ADDRESS, (byte) 0xa7);

        assertThat(listenerCalled.get()).isTrue();
    }
}