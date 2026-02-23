package org.gameboy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.gameboy.common.MappedMemory;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryInitializer;
import org.gameboy.cpu.Cpu;
import org.gameboy.io.EmulatorWindow;

import java.io.IOException;

public class Main {
    private static final String DEFAULT_BOOT_ROM = "dmg_boot.bin";
    private static final String ACID_2_ROM = "src/test/resources/dmg-acid2.gb";
    private static final String DEFAULT_GAME_ROM = "roms/Tetris (Japan) (En).gb";

    public static void main(String[] args) {
        try {
            String bootRomPath = args.length > 0 ? args[0] : null;
            String gameRomPath = args.length > 1 ? args[1] : DEFAULT_GAME_ROM;

            Injector injector = Guice.createInjector(new EmulatorModule());

            MemoryInitializer memoryInitializer = injector.getInstance(MemoryInitializer.class);
            var memoryDumps = memoryInitializer.createMemoryDumps(bootRomPath, gameRomPath);

            Memory underlyingMemory = injector.getInstance(Key.get(Memory.class, Names.named("underlying")));
            if (underlyingMemory instanceof MappedMemory mappedMemory) {
                mappedMemory.loadMemoryDumps(memoryDumps);
            }

            Cpu cpu = injector.getInstance(Cpu.class);
            EmulatorWindow emulatorWindow = injector.getInstance(EmulatorWindow.class);

            emulatorWindow.show();

            System.out.println("Starting emulation...");
            System.out.println("Boot ROM: " + (bootRomPath != null ? bootRomPath : "skipped"));
            System.out.println("Game ROM: " + gameRomPath);

            int frameCounter = 0;
            while (true) {
                cpu.cycle();

                if (++frameCounter % 70224 == 0) {
                    emulatorWindow.refreshDebug();
                }
            }

        } catch (IOException e) {
            System.err.println("Error loading ROM files: " + e.getMessage());
            System.err.println("Usage: java -jar gameboy-emulator.jar [boot_rom_path] [game_rom_path]");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
