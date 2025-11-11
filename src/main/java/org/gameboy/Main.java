package org.gameboy;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.gameboy.common.MappedMemory;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryInitializer;
import org.gameboy.common.SimpleMemory;
import org.gameboy.cpu.Cpu;
import org.gameboy.display.Display;
import org.gameboy.display.PpuRegisters;
import org.gameboy.display.WindowDisplay;
import org.gameboy.display.debug.VramDebugWindow;

import javax.swing.*;
import java.awt.*;
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
            
            Memory memory = injector.getInstance(Memory.class);
            if (memory instanceof MappedMemory mappedMemory) {
                mappedMemory.loadMemoryDumps(memoryDumps);
            } else if (memory instanceof SimpleMemory simpleMemory) {
                simpleMemory.loadMemoryDumps(memoryDumps);
            }
            
            Display display = injector.getInstance(Display.class);
            Cpu cpu = injector.getInstance(Cpu.class);
            PpuRegisters ppuRegisters = injector.getInstance(PpuRegisters.class);

            VramDebugWindow debugWindow = new VramDebugWindow(memory, ppuRegisters);

            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Game Boy Emulator");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(500, 500);

                if (display instanceof WindowDisplay windowDisplay) {
                    frame.add(windowDisplay, BorderLayout.CENTER);
                }

                frame.setVisible(true);

                debugWindow.setLocation(frame.getX() + frame.getWidth() + 10, frame.getY());
                debugWindow.showWindow();
            });
            
            System.out.println("Starting emulation...");
            System.out.println("Boot ROM: " + (bootRomPath != null ? bootRomPath : "skipped"));
            System.out.println("Game ROM: " + gameRomPath);

            int frameCounter = 0;
            while (true) {
                cpu.cycle();

                if (++frameCounter % 60 == 0) {
                    SwingUtilities.invokeLater(debugWindow::refresh);
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