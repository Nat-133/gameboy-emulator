package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.gameboy.common.annotations.*;
import org.gameboy.display.annotations.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.gameboy.common.MemoryMapConstants.DMA_REGISTER_ADDRESS;
import static org.gameboy.utils.BitUtilities.uint;

@Singleton
public class MappedMemory implements Memory {
    private final MemoryLocation[] memoryMap = new MemoryLocation[0x10000];
    private final byte[] defaultMemory = new byte[0x10000];
    private final ConcurrentMap<Short, MemoryListener> memoryListeners = new ConcurrentHashMap<>();
    private final Cartridge cartridge;

    @Inject
    public MappedMemory(Cartridge cartridge,
                       @Div ByteRegister divRegister,
                       @Tima ByteRegister timaRegister,
                       @Tma ByteRegister tmaRegister,
                       @Tac ByteRegister tacRegister,
                       @Dma ByteRegister dmaRegister,
                       @InterruptFlags ByteRegister interruptFlagsRegister,
                       @InterruptEnable ByteRegister interruptEnableRegister,
                       @Lcdc ByteRegister lcdcRegister,
                       @Stat ByteRegister statRegister,
                       @Scy ByteRegister scyRegister,
                       @Scx ByteRegister scxRegister,
                       @Ly ByteRegister lyRegister,
                       @Lyc ByteRegister lycRegister,
                       @Wy ByteRegister wyRegister,
                       @Wx ByteRegister wxRegister,
                       @Bgp ByteRegister bgpRegister,
                       @Obp0 ByteRegister obp0Register,
                       @Obp1 ByteRegister obp1Register,
                       SerialController serialController,
                       @org.gameboy.common.annotations.Joypad ByteRegister joypadRegister,
                       @org.gameboy.audio.annotations.ApuRegisters Map<Integer, ByteRegister> apuRegisters) {
        this.cartridge = cartridge;

        memoryMap[0xFF00] = new ByteRegisterMapping(joypadRegister);
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

        // APU registers (0xFF10-0xFF26 + wave RAM 0xFF30-0xFF3F)
        for (Map.Entry<Integer, ByteRegister> entry : apuRegisters.entrySet()) {
            memoryMap[entry.getKey()] = new ByteRegisterMapping(entry.getValue());
        }
    }

    @Override
    public byte read(short address) {
        int addr = uint(address);
        MemoryLocation mappedValue = memoryMap[addr];
        if (mappedValue != null) {
            return mappedValue.read();
        }
        if (isCartridgeAddress(addr)) {
            return cartridge.read(address);
        }
        return defaultMemory[addr];
    }

    @Override
    public void write(short address, byte value) {
        int addr = uint(address);
        MemoryLocation mappedValue = memoryMap[addr];
        if (mappedValue != null) {
            mappedValue.write(value);
        } else if (isCartridgeAddress(addr)) {
            cartridge.write(address, value);
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
    
    private static boolean isCartridgeAddress(int addr) {
        return (addr <= 0x7FFF) || (addr >= 0xA000 && addr <= 0xBFFF);
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
