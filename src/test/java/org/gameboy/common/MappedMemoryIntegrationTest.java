package org.gameboy.common;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.EmulatorModule;
import org.gameboy.display.PpuRegisters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappedMemoryIntegrationTest {

    private Memory memory;
    private PpuRegisters ppuRegisters;

    @BeforeEach
    public void setUp() {
        Injector injector = Guice.createInjector(new EmulatorModule(false));
        memory = injector.getInstance(Memory.class);
        ppuRegisters = injector.getInstance(PpuRegisters.class);
    }

    @Test
    public void givenProductionInjection_whenScxMemoryAddressIsWritten_thenScxRegisterIsUpdated() {
        byte expectedValue = (byte) 0x42;
        memory.write((short) 0xFF43, expectedValue);

        assertEquals(expectedValue, ppuRegisters.read(PpuRegisters.PpuRegister.SCX));
    }

    @Test
    public void givenProductionInjection_whenScyMemoryAddressIsWritten_thenScyRegisterIsUpdated() {
        byte expectedValue = (byte) 0x89;
        memory.write((short) 0xFF42, expectedValue);

        assertEquals(expectedValue, ppuRegisters.read(PpuRegisters.PpuRegister.SCY));
    }

    @Test
    public void givenProductionInjection_whenScxRegisterIsWritten_thenMemoryReflectsChange() {
        byte expectedValue = (byte) 0xAB;
        ppuRegisters.write(PpuRegisters.PpuRegister.SCX, expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF43));
    }

    @Test
    public void givenProductionInjection_whenScyRegisterIsWritten_thenMemoryReflectsChange() {
        byte expectedValue = (byte) 0xCD;
        ppuRegisters.write(PpuRegisters.PpuRegister.SCY, expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF42));
    }
}
