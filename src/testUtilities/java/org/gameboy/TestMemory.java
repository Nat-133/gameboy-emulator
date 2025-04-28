package org.gameboy;

import org.gameboy.common.Memory;
import org.gameboy.common.MemoryListener;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.gameboy.utils.BitUtilities.uint;

public class TestMemory implements Memory {
    private final byte[] memory;
    private final ConcurrentMap<Short, MemoryListener> memoryListeners = new ConcurrentHashMap<>();

    public TestMemory() {
        memory = new byte[0xFFFF+1];
    }

    public TestMemory withMemoryDump(MemoryDump memoryDump) {
        System.arraycopy(memoryDump.memory(), 0, memory, uint(memoryDump.startAddress()), memoryDump.length());
        return this;
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
