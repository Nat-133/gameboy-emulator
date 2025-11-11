package org.gameboy.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class DmaControllerTest {
    private Memory memory;
    private MemoryBus dmaController;

    @BeforeEach
    void setUp() {
        memory = Mockito.mock(Memory.class);
        dmaController = new MemoryBus(memory);
    }

    @Test
    void shouldStartInactive() {
        assertThat(dmaController.isDmaActive()).isFalse();
    }

    @Test
    void shouldActivateWhenDmaStarted() {
        dmaController.startDma((byte) 0xC0);

        assertThat(dmaController.isDmaActive()).isTrue();
    }

    @Test
    void shouldTransferDataFromSourceToOamDuringDma() {
        for (int i = 0; i < 160; i++) {
            short sourceAddr = (short) (0xC000 + i);
            byte value = (byte) (i & 0xFF);
            Mockito.when(memory.read(sourceAddr)).thenReturn(value);
        }

        dmaController.startDma((byte) 0xC0);

        for (int i = 0; i < 161; i++) {
            dmaController.mCycle();
        }

        for (int i = 0; i < 160; i++) {
            short oamAddr = (short) (0xFE00 + i);
            byte expectedValue = (byte) (i & 0xFF);
            Mockito.verify(memory).write(oamAddr, expectedValue);
        }
    }

    @Test
    void shouldDeactivateDmaAfter161Cycles() {
        dmaController.startDma((byte) 0xC0);

        for (int i = 0; i < 161; i++) {
            dmaController.mCycle();
        }

        assertThat(dmaController.isDmaActive()).isFalse();
    }

    @Test
    void shouldRestartDmaWhenTriggeredDuringActiveDma() {
        dmaController.startDma((byte) 0xC0);
        dmaController.mCycle();
        dmaController.mCycle();

        dmaController.startDma((byte) 0xD0);

        for (int i = 0; i < 160; i++) {
            short sourceAddr = (short) (0xD000 + i);
            byte value = (byte) (i + 100);
            Mockito.when(memory.read(sourceAddr)).thenReturn(value);
        }

        for (int i = 0; i < 161; i++) {
            dmaController.mCycle();
        }

        for (int i = 0; i < 160; i++) {
            short oamAddr = (short) (0xFE00 + i);
            byte expectedValue = (byte) (i + 100);
            Mockito.verify(memory).write(oamAddr, expectedValue);
        }

        assertThat(dmaController.isDmaActive()).isFalse();
    }

    @Test
    void shouldHaveOneCycleDelayBeforeFirstTransfer() {
        Memory testMemory = Mockito.mock(Memory.class);
        MemoryBus testDma = new MemoryBus(testMemory);

        Mockito.when(testMemory.read((short) 0xC000)).thenReturn((byte) 0x42);

        testDma.startDma((byte) 0xC0);

        testDma.mCycle();
        Mockito.verify(testMemory, Mockito.never()).write((short) 0xFE00, (byte) 0x42);

        testDma.mCycle();
        Mockito.verify(testMemory).write((short) 0xFE00, (byte) 0x42);
    }

    @Test
    void shouldTake161CyclesTotal() {
        for (int i = 0; i < 160; i++) {
            Mockito.when(memory.read((short) (0xC000 + i))).thenReturn((byte) i);
        }

        dmaController.startDma((byte) 0xC0);

        for (int i = 0; i < 160; i++) {
            dmaController.mCycle();
        }
        assertThat(dmaController.isDmaActive()).isTrue();

        dmaController.mCycle();
        assertThat(dmaController.isDmaActive()).isFalse();
    }
}
