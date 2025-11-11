package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.gameboy.common.annotations.*;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.gameboy.common.MemoryMapConstants.DMA_REGISTER_ADDRESS;
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
                       @Tac ByteRegister tacRegister,
                       @Dma ByteRegister dmaRegister,
                       @InterruptFlags ByteRegister interruptFlagsRegister,
                       @InterruptEnable ByteRegister interruptEnableRegister,
                       @Named("lcdc") ByteRegister lcdcRegister,
                       @Named("stat") ByteRegister statRegister,
                       @Named("scy") ByteRegister scyRegister,
                       @Named("scx") ByteRegister scxRegister,
                       @Named("ly") ByteRegister lyRegister,
                       @Named("lyc") ByteRegister lycRegister,
                       @Named("wy") ByteRegister wyRegister,
                       @Named("wx") ByteRegister wxRegister,
                       @Named("bgp") ByteRegister bgpRegister,
                       @Named("obp0") ByteRegister obp0Register,
                       @Named("obp1") ByteRegister obp1Register,
                       SerialController serialController) {

        memoryMap[0xFF01] = new SerialDataMapping(serialController);
        memoryMap[0xFF02] = new SerialControlMapping(serialController);
        
        memoryMap[0xFF04] = new ByteRegisterMapping(divRegister);
        memoryMap[0xFF05] = new ByteRegisterMapping(timaRegister);
        memoryMap[0xFF06] = new ByteRegisterMapping(tmaRegister);
        memoryMap[0xFF07] = new ByteRegisterMapping(tacRegister);
        memoryMap[0xFF0F] = new ByteRegisterMapping(interruptFlagsRegister);

        memoryMap[DMA_REGISTER_ADDRESS & 0xFFFF] = new ByteRegisterMapping(dmaRegister);

        memoryMap[0xFF40] = new ByteRegisterMapping(lcdcRegister);
        memoryMap[0xFF41] = new ByteRegisterMapping(statRegister);
        memoryMap[0xFF42] = new ByteRegisterMapping(scyRegister);
        memoryMap[0xFF43] = new ByteRegisterMapping(scxRegister);
        memoryMap[0xFF44] = new ByteRegisterMapping(lyRegister);
        memoryMap[0xFF45] = new ByteRegisterMapping(lycRegister);
        memoryMap[0xFF47] = new ByteRegisterMapping(bgpRegister);
        memoryMap[0xFF48] = new ByteRegisterMapping(obp0Register);
        memoryMap[0xFF49] = new ByteRegisterMapping(obp1Register);
        memoryMap[0xFF4A] = new ByteRegisterMapping(wyRegister);
        memoryMap[0xFF4B] = new ByteRegisterMapping(wxRegister);
        memoryMap[0xFFFF] = new ByteRegisterMapping(interruptEnableRegister);
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
    
    private record SerialDataMapping(SerialController serialController) implements MemoryLocation {
        @Override
        public byte read() {
            return serialController.readSerialData();
        }
        
        @Override
        public void write(byte value) {
            serialController.writeSerialData(value);
        }
    }
    
    private record SerialControlMapping(SerialController serialController) implements MemoryLocation {
        @Override
        public byte read() {
            return serialController.readSerialControl();
        }
        
        @Override
        public void write(byte value) {
            serialController.writeSerialControl(value);
        }
    }
}
