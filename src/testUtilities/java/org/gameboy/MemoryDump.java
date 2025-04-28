package org.gameboy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.gameboy.utils.BitUtilities.uint;

public record MemoryDump(short startAddress, byte[] memory) {
    public static MemoryDump from(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        Pattern filenamePattern = Pattern.compile(
                "^(?<name>.+?)_vmem_(?<start>[0-9A-Fa-f]{4})_(?<end>[0-9A-Fa-f]{4})\\.txt$"
        );
        Matcher filenameMatcher = filenamePattern.matcher(fileName);

        if (!filenameMatcher.matches()) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }

        String startAddressStr = filenameMatcher.group("start");
        String endAddressStr = filenameMatcher.group("end");
        short startAddress = (short) Integer.parseInt(startAddressStr, 16);
        short endAddress = (short) Integer.parseInt(endAddressStr, 16);

        List<String> strings = Files.readAllLines(path);
        List<Integer> intList = strings.stream()
                .flatMap(s -> Arrays.stream(s.split(" ")))
                .map(s -> Integer.parseInt(s, 16))
                .toList();
        byte[] memory = new byte[intList.size()];
        for (int i = 0; i < memory.length; i++) {
            memory[i] = (byte) (int) intList.get(i);
        }
        int actualLength = memory.length;
        int expectedLength = uint(endAddress) - uint(startAddress);
        if (actualLength != expectedLength) {
//            throw new IllegalArgumentException("Memory dump has different length than expected");
            System.out.println("Memory dump has different length than expected");
        }

        return new MemoryDump(startAddress, memory);
    }

    public int length() {
        return memory.length;
    }
}
