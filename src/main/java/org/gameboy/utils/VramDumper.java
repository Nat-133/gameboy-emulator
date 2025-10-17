package org.gameboy.utils;

import org.gameboy.common.Memory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class VramDumper {
    private static final int VRAM_START = 0x8000;
    private static final int VRAM_END = 0x97FF;
    private static final int VRAM_SIZE = VRAM_END - VRAM_START + 1; // 6144 bytes

    private VramDumper() {
        // Private constructor for utility class
    }

    /**
     * Dumps VRAM memory (0x8000-0x97FF) to a binary file for visualization
     * Compatible with https://dtabacaru.com/vram/
     *
     * @param memory The memory to dump from
     * @param filename The output filename (e.g., "dump.bin")
     */
    public static void dumpVram(Memory memory, String filename) {
        Path outputPath = Paths.get(filename);

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] vramData = new byte[VRAM_SIZE];

            // Read VRAM data from memory
            for (int i = 0; i < VRAM_SIZE; i++) {
                vramData[i] = memory.read((short)(VRAM_START + i));
            }

            // Write to file
            fos.write(vramData);

            System.out.println("VRAM dumped to " + outputPath.toAbsolutePath());
            System.out.println("Upload to https://dtabacaru.com/vram/ to visualize");
        } catch (IOException e) {
            System.err.println("Failed to dump VRAM: " + e.getMessage());
        }
    }

    /**
     * Dumps full memory range to a binary file
     *
     * @param memory The memory to dump from
     * @param startAddress Start address (inclusive)
     * @param endAddress End address (inclusive)
     * @param filename The output filename
     */
    public static void dumpMemoryRange(Memory memory, int startAddress, int endAddress, String filename) {
        Path outputPath = Paths.get(filename);
        int size = endAddress - startAddress + 1;

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] data = new byte[size];

            // Read memory data
            for (int i = 0; i < size; i++) {
                data[i] = memory.read((short)(startAddress + i));
            }

            // Write to file
            fos.write(data);

            System.out.printf("Memory range 0x%04X-0x%04X dumped to %s%n",
                startAddress, endAddress, outputPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to dump memory: " + e.getMessage());
        }
    }
}