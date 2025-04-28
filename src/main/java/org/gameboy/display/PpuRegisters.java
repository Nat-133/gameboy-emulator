package org.gameboy.display;

import org.gameboy.common.ByteRegister;
import org.gameboy.common.IntBackedRegister;

import java.util.EnumMap;
import java.util.Map;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;

public class PpuRegisters {
    private final Map<PpuRegister, ByteRegister> registerMap;

    public PpuRegisters() {
        registerMap = new EnumMap<>(PpuRegister.class);
        registerMap.put(LY, new IntBackedRegister());
        registerMap.put(SCX, new IntBackedRegister());
        registerMap.put(SCY, new IntBackedRegister());
        registerMap.put(WX, new IntBackedRegister());
        registerMap.put(WY, new IntBackedRegister());
        registerMap.put(LCDC, new IntBackedRegister());
        registerMap.put(STAT, new IntBackedRegister());
    }

    public byte read(PpuRegister register) {
        return registerMap.get(register).read();
    }

    public void write(PpuRegister register, byte value) {
        registerMap.get(register).write(value);
    }

    public enum PpuRegister {
        LY,
        SCX,
        SCY,
        WX,
        WY,
        LCDC,
        STAT,
    }
}
