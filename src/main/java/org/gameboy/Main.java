package org.gameboy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.common.Cartridge;
import org.gameboy.common.RomLoader;
import org.gameboy.cartridge.CartridgeFactory;
import org.gameboy.cpu.Cpu;
import org.gameboy.io.EmulatorWindow;

import java.io.IOException;

public class Main {
    private static final String DEFAULT_GAME_ROM = "roms/pokemon_blue.gb";

    public static void main(String[] args) {
        try {
            String gameRomPath = args.length > 0 ? args[0] : DEFAULT_GAME_ROM;

            RomLoader romLoader = new RomLoader();
            byte[] gameRom = romLoader.loadRom(gameRomPath);
            Cartridge cartridge = CartridgeFactory.fromRom(gameRom);

            Injector injector = Guice.createInjector(new EmulatorModule(cartridge));

            Cpu cpu = injector.getInstance(Cpu.class);
            EmulatorWindow emulatorWindow = injector.getInstance(EmulatorWindow.class);

            System.out.println("Game ROM: " + gameRomPath);

            emulatorWindow.run(cpu);

        } catch (IOException e) {
            System.err.println("Error loading ROM files: " + e.getMessage());
            System.err.println("Usage: java -jar gameboy-emulator.jar [game_rom_path]");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
