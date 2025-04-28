package org.gameboy.display;

import org.gameboy.MemoryDump;
import org.gameboy.TestMemory;
import org.gameboy.common.Memory;
import org.gameboy.common.SynchronisedClock;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Main {
    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);
        var display = new WindowDisplay();
//        display.setBorder(BorderFactory.createLineBorder(Color.RED));
        frame.add(display, BorderLayout.CENTER);
        frame.setVisible(true);
//        frame.pack();

        PpuRegisters registers = new PpuRegisters();
        PixelFifo backgroundFifo = new PixelFifo();
        SynchronisedClock ppuClock = new SynchronisedClock();
//        MemoryDump tetrisVram = MemoryDump.from(Paths.get("/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/test/resources/tetris_vmem_8000_9fff.txt"));
        byte[] mario_memory = loadHexMemoryDump();
        MemoryDump mario_vram = new MemoryDump((short) 0, mario_memory);
        Memory memory = new TestMemory().withMemoryDump(mario_vram);
        ppuClock.registerParallelOperation();
        var backgroundFetcher = new BackgroundFetcher(memory, registers, backgroundFifo, ppuClock);
        var scanlineController = new ScanlineController(ppuClock, display, backgroundFifo, new PixelCombinator(),registers, backgroundFetcher);
        var ppu = new PictureProcessingUnit(scanlineController);


        Executor executor = new ScheduledThreadPoolExecutor(1);
        executor.execute(backgroundFetcher::runFetcher);

        while (true) {
            ppu.renderAllScanlines();
            frame.revalidate();
        }
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