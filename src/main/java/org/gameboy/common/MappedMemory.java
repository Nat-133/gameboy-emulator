package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.common.annotations.Div;
import org.gameboy.common.annotations.Tac;
import org.gameboy.common.annotations.Tima;
import org.gameboy.common.annotations.Tma;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.gameboy.utils.BitUtilities.uint;

@Singleton
public class MappedMemory implements Memory {
    private final MemoryLocation[] memoryMap = new MemoryLocation[0x10000];
    private final byte[] defaultMemory = new byte[0x10000];
    private final ConcurrentMap<Short, MemoryListener> memoryListeners = new ConcurrentHashMap<>();

    @Inject
    public MappedMemory(@Div ByteRegister divRegister, 
                       @Tima ByteRegister timaRegister, 
                       @Tma ByteRegister tmaRegister, 
                       @Tac ByteRegister tacRegister) {
        memoryMap[0xFF04] = new ByteRegisterMapping(divRegister);
        memoryMap[0xFF05] = new ByteRegisterMapping(timaRegister);
        memoryMap[0xFF06] = new ByteRegisterMapping(tmaRegister);
        memoryMap[0xFF07] = new ByteRegisterMapping(tacRegister);
    }

    @Override
    public byte read(short address) {
        int addr = uint(address);
        MemoryLocation mappedValue = memoryMap[addr];
        return (mappedValue != null) ? mappedValue.read() : defaultMemory[addr];
    }

    @Override
    public void write(short address, byte value) {
        int addr = uint(address);
        MemoryLocation mappedValue = memoryMap[addr];
        if (mappedValue != null) {
            mappedValue.write(value);
        } else {
            defaultMemory[addr] = value;
        }

        MemoryListener memoryListener = memoryListeners.getOrDefault(address, () -> {});
        memoryListener.onMemoryWrite();
    }

    @Override
    public void registerMemoryListener(short address, MemoryListener listener) {
        memoryListeners.put(address, listener);
    }
    
    public void loadMemoryDumps(List<MemoryDump> memoryDumps) {
        for (MemoryDump dump : memoryDumps) {
            System.arraycopy(dump.memory(), 0, defaultMemory, uint(dump.startAddress()), dump.length());
        }
    }

    private interface MemoryLocation {
        byte read();

        void write(byte value);
    }

    private record ByteRegisterMapping(ByteRegister mappedRegister) implements MemoryLocation {
        @Override
        public byte read() {
            return mappedRegister.read();
        }

        @Override
        public void write(byte value) {
            mappedRegister.write(value);
        }
    }
}
