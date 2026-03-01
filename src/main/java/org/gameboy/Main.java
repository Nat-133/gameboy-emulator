package org.gameboy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.cartridge.CartridgeFactory;
import org.gameboy.common.Cartridge;
import org.gameboy.common.RomLoader;
import org.gameboy.cpu.Cpu;
import org.gameboy.io.EmulatorWindow;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final Path ROMS_DIR = Path.of("roms");
    private static final Path DEFAULT_ROM = Path.of("shocklobster.gb");

    public static void main(String[] args) {
        try {
            Path romPath = args.length > 0 ? resolveRomPath(args[0]) : defaultRom();

            RomLoader romLoader = new RomLoader();
            byte[] gameRom = romLoader.loadRom(romPath.toString());
            Cartridge cartridge = CartridgeFactory.fromRom(gameRom);

            Injector injector = Guice.createInjector(new EmulatorModule(cartridge));

            Cpu cpu = injector.getInstance(Cpu.class);
            EmulatorWindow emulatorWindow = injector.getInstance(EmulatorWindow.class);

            System.out.println("Game ROM: " + romPath);

            emulatorWindow.run(cpu);

        } catch (IOException e) {
            System.err.println("Error loading ROM files: " + e.getMessage());
            System.err.println("Usage: java -jar gameboy-emulator.jar [rom_path]");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path resolveRomPath(String input) throws IOException {
        Path path = Path.of(input);

        if (path.isAbsolute()) {
            if (Files.isRegularFile(path)) {
                return path;
            }
            throw new IOException("ROM not found: " + path);
        }

        // Check current working directory
        if (Files.isRegularFile(path)) {
            return path;
        }

        // Check roms directory
        Path inRomsDir = ROMS_DIR.resolve(path);
        if (Files.isRegularFile(inRomsDir)) {
            return inRomsDir;
        }

        throw new IOException("ROM not found: " + input + " (searched CWD and " + ROMS_DIR + "/)");
    }

    private static Path defaultRom() throws IOException {
        // Check roms/ directory first (user's own ROMs take priority)
        if (Files.isDirectory(ROMS_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(ROMS_DIR, "*.gb")) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        return entry;
                    }
                }
            }
        }

        // Fall back to bundled default ROM
        if (Files.isRegularFile(DEFAULT_ROM)) {
            return DEFAULT_ROM;
        }

        throw new IOException("No ROM found. Place .gb files in roms/ or provide a path as argument.");
    }
}
