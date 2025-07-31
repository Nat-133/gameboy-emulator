package org.gameboy.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.gameboy.utils.BitUtilities.uint;

public class SimpleMemory implements Memory {
    private final byte[] memory;
    private final ConcurrentMap<Short, MemoryListener> memoryListeners = new ConcurrentHashMap<>();

    public SimpleMemory() {
        memory = new byte[0xFFFF + 1];
    }

    @Override
    public byte read(short address) {
        return memory[uint(address)];
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
    
    public void loadMemoryDumps(java.util.List<MemoryDump> memoryDumps) {
        for (MemoryDump dump : memoryDumps) {
            System.arraycopy(dump.memory(), 0, memory, uint(dump.startAddress()), dump.length());
        }
    }
}