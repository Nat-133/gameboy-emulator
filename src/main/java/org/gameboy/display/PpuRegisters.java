package org.gameboy.display;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gameboy.common.ByteRegister;

import java.util.EnumMap;
import java.util.Map;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;

public class PpuRegisters {
    private final Map<PpuRegister, ByteRegister> registerMap;

    @Inject
    public PpuRegisters(@Named("ly") ByteRegister ly,
                        @Named("lyc") ByteRegister lyc,
                        @Named("scx") ByteRegister scx,
                        @Named("scy") ByteRegister scy,
                        @Named("wx") ByteRegister wx,
                        @Named("wy") ByteRegister wy,
                        @Named("lcdc") ByteRegister lcdc,
                        @Named("stat") ByteRegister stat,
                        @Named("bgp") ByteRegister bgp,
                        @Named("obp0") ByteRegister obp0,
                        @Named("obp1") ByteRegister obp1) {
        registerMap = new EnumMap<>(PpuRegister.class);
        registerMap.put(LY, ly);
        registerMap.put(LYC, lyc);
        registerMap.put(SCX, scx);
        registerMap.put(SCY, scy);
        registerMap.put(WX, wx);
        registerMap.put(WY, wy);
        registerMap.put(LCDC, lcdc);
        registerMap.put(STAT, stat);
        registerMap.put(BGP, bgp);
        registerMap.put(OBP0, obp0);
        registerMap.put(OBP1, obp1);
    }

    public byte read(PpuRegister register) {
        return registerMap.get(register).read();
    }

    public void write(PpuRegister register, byte value) {
        registerMap.get(register).write(value);
    }

    public enum PpuRegister {
        LY,
        LYC,
        SCX,
        SCY,
        WX,
        WY,
        LCDC,
        STAT,
        BGP,
        OBP0,
        OBP1,
    }
}
