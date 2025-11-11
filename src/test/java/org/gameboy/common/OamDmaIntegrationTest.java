package org.gameboy.common;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.EmulatorModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OamDmaIntegrationTest {
    private Memory memory;
    private DmaController dmaController;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(new EmulatorModule());
        memory = injector.getInstance(Memory.class);
        dmaController = injector.getInstance(DmaController.class);
    }

    @Test
    void shouldTransferDataFromSourceToOamWhenFF46Written() {
        for (int i = 0; i < 160; i++) {
            memory.write((short) (0xC000 + i), (byte) (i & 0xFF));
        }

        memory.write((short) 0xFF46, (byte) 0xC0);

        for (int i = 0; i < 161; i++) {
            dmaController.mCycle();
        }

        for (int i = 0; i < 160; i++) {
            byte actual = memory.read((short) (0xFE00 + i));
            assertThat(actual).isEqualTo((byte) (i & 0xFF));
        }
    }

    @Test
    void shouldBlockNonHramAccessDuringDma() {
        memory.write((short) 0xFF46, (byte) 0xC0);

        byte result = memory.read((short) 0xC000);
        assertThat(result).isEqualTo((byte) 0xFF);

        memory.write((short) 0xFF80, (byte) 0x42);
        byte hramResult = memory.read((short) 0xFF80);
        assertThat(hramResult).isEqualTo((byte) 0x42);
    }

    @Test
    void shouldAllowNormalAccessAfterDmaCompletes() {
        memory.write((short) 0xC000, (byte) 0x42);

        memory.write((short) 0xFF46, (byte) 0xD0);
        for (int i = 0; i < 161; i++) {
            dmaController.mCycle();
        }

        byte result = memory.read((short) 0xC000);
        assertThat(result).isEqualTo((byte) 0x42);
    }
}
