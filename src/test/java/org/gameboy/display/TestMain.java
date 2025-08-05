package org.gameboy.display;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.MemoryDump;
import org.gameboy.TestMemory;
import org.gameboy.common.*;
import org.gameboy.cpu.Cpu;
import org.gameboy.cpu.components.CpuStructure;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TestMain {
    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        var display = new WindowDisplay();
//        display.setBorder(BorderFactory.createLineBorder(Color.RED));
        frame.add(display, BorderLayout.CENTER);
        frame.setVisible(true);
//        frame.pack();
        byte[] boot_rom_memory = loadGbRom("dmg_boot.bin");

        byte[] test_memory = loadGbRom("dmg-acid2.gb");
        byte[] mario_memory = loadHexMemoryDump();
        MemoryDump mario_vram = new MemoryDump((short) 0, test_memory);
        MemoryDump boot_rom = new MemoryDump((short) 0, boot_rom_memory);
        Memory memory = new TestMemory()
                .withMemoryDump(mario_vram)
                .withMemoryDump(boot_rom);
        ObjectAttributeMemory oam = new ObjectAttributeMemory(memory);

        PpuRegisters registers = new PpuRegisters(
                new IntBackedRegister(), // ly
                new IntBackedRegister(), // lyc
                new IntBackedRegister(), // scx
                new IntBackedRegister(), // scy
                new IntBackedRegister(), // wx
                new IntBackedRegister(), // wy
                new IntBackedRegister(), // lcdc
                new IntBackedRegister()  // stat
        );
//        registers.write(PpuRegisters.PpuRegister.SCX, (byte)(8*27));
        PixelFifo backgroundFifo = new PixelFifo();
        PixelFifo spriteFifo = new PixelFifo();
        SynchronisedClock ppuClock = new SynchronisedClock();
//        MemoryDump tetrisVram = MemoryDump.from(Paths.get("/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/test/resources/tetris_vmem_8000_9fff.txt"));

        SpriteBuffer spriteBuffer = new SpriteBuffer();

        var backgroundFetcher = new BackgroundFetcher(memory, registers, backgroundFifo, ppuClock);
        var spriteFetcher = new SpriteFetcher(spriteBuffer, memory, registers, spriteFifo, ppuClock);
        var scanlineController = new ScanlineController(ppuClock, display, backgroundFifo, spriteFifo, new PixelCombinator(), registers, backgroundFetcher, spriteFetcher, spriteBuffer);
        var oamScanner = new OamScanController(oam, ppuClock, spriteBuffer, registers);
        var interruptController = new InterruptController(memory);
        var ppu = new PictureProcessingUnit(scanlineController, registers, ppuClock, oamScanner, new DisplayInterruptController(interruptController, registers));

        Clock cpuClock = new ClockWithParallelProcess(() -> {
            for (int i=0; i<4; i++) {
                ppu.tCycle();
            }
        });
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(memory)
                .withClock(cpuClock)
                .withPC(0x100)
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        while (true) {
            cpu.cycle();
//            Thread.sleep(10);
        }
    }

    private static byte[] loadGbRom(String filepath) throws IOException {
        Path filePath = Paths.get(filepath);

        return Files.readAllBytes(filePath);
    }

    static byte[] loadHexMemoryDump() throws IOException {
        // Replace with the path to your .export file
//        Path filePath = Paths.get("/Users/nathaniel.manley/Downloads/gameboy_color_saves.export");
        Path filePath = Paths.get("/Users/nathaniel.manley/vcs/personal/gameboy-emulator/super_mario_memory_2.txt");

        String hex = Files.readString(filePath).replaceAll("\\s", "");
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even number of characters.");
        }

        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            result[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }

        return result;
    }

    static byte[] loadGameboyOnlineMemory() throws IOException {
        // Replace with the path to your .export file
        String filePath = "/Users/nathaniel.manley/Downloads/gameboy_color_saves.export";

        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        int start = content.indexOf('[');
        start = content.indexOf('[', start+1);
        int end = content.indexOf(']', start);

        if (start != -1 && end != -1) {
            String byteString = content.substring(start + 1, end);
            String[] byteValues = byteString.split(",");

            ArrayList<Byte> byteList = new ArrayList<>();
            for (String byteVal : byteValues) {
                byteList.add((byte) Integer.parseInt(byteVal.trim()));
            }

            // Convert to byte[]
            byte[] byteArray = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                byteArray[i] = byteList.get(i);
            }
            return byteArray;
        }

        return null;
    }
}