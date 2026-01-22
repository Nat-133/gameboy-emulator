package org.gameboy.common;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.gameboy.EmulatorModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.gameboy.GameboyAssertions.assertThatHex;

/**
 * Integration test verifying ROM loading works correctly with production DI setup.
 * Bug discovered: Main.java gets Memory.class which returns MemoryBus, but then
 * checks instanceof MappedMemory/SimpleMemory which both fail, silently skipping
 * ROM loading.
 */
public class MemoryInitializerIntegrationTest {

    private Injector injector;

    @BeforeEach
    public void setUp() {
        injector = Guice.createInjector(new EmulatorModule());
    }

    @Test
    public void givenRomData_whenLoadedToUnderlyingMemory_thenDataIsAccessible() {
        // Arrange: Create ROM data with recognizable pattern
        // Simulate a minimal Game Boy ROM header area
        byte[] romData = new byte[0x200];  // 512 bytes
        romData[0x100] = 0x00;             // NOP at entry point
        romData[0x101] = (byte) 0xC3;      // JP instruction
        romData[0x102] = 0x50;             // Low byte of jump target
        romData[0x103] = 0x01;             // High byte - JP $0150
        romData[0x150] = (byte) 0x3E;      // LD A, n at $0150
        romData[0x151] = 0x42;             // immediate value

        List<MemoryDump> memoryDumps = List.of(MemoryDump.fromZero(romData));

        // Act: Load using @Named("underlying") - the correct way (like BlarggTestRunner)
        Memory underlyingMemory = injector.getInstance(Key.get(Memory.class, Names.named("underlying")));
        if (underlyingMemory instanceof MappedMemory mappedMemory) {
            mappedMemory.loadMemoryDumps(memoryDumps);
        }

        // Assert: Data should be accessible via the default Memory binding (MemoryBus)
        Memory memory = injector.getInstance(Memory.class);

        assertThatHex(memory.read((short) 0x100)).isEqualTo((byte) 0x00);  // NOP
        assertThatHex(memory.read((short) 0x101)).isEqualTo((byte) 0xC3);  // JP
        assertThatHex(memory.read((short) 0x102)).isEqualTo((byte) 0x50);  // low byte
        assertThatHex(memory.read((short) 0x103)).isEqualTo((byte) 0x01);  // high byte
        assertThatHex(memory.read((short) 0x150)).isEqualTo((byte) 0x3E);  // LD A, n
        assertThatHex(memory.read((short) 0x151)).isEqualTo((byte) 0x42);  // immediate
    }
}
