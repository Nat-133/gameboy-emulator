package org.gameboy.components;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.gameboy.utils.BitUtilities.uint;

public class BasicMemory implements Memory {
    private final byte[] memory;
    private final ConcurrentMap<Short, MemoryListener> memoryListeners = new ConcurrentHashMap<>();

    public BasicMemory() {
        memory = new byte[0xFFFF+1];
    }

    @Override
    public byte read(short address) {
        int firstAddress = uint(address);
        return memory[firstAddress];
    }

    @Override
    public void write(short address, byte value) {
        memory[uint(address)] = value;

        MemoryListener memoryListener = memoryListeners.getOrDefault(address, () -> {});
        memoryListener.onMemoryWrite();
    }

    @Override
    public void registerMemoryListener(short address, MemoryListener listener) {
        memoryListeners.put(address, listener);
    }

}
