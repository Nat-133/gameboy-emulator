package org.gameboy.display;

import com.google.inject.Inject;
import org.gameboy.common.ByteRegister;
import org.gameboy.display.annotations.*;

import java.util.EnumMap;
import java.util.Map;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;

public class PpuRegisters {
    private final Map<PpuRegister, ByteRegister> registerMap;
    private final StatRegister statRegister;

    @Inject
    public PpuRegisters(@Ly ByteRegister ly,
                        @Lyc ByteRegister lyc,
                        @Scx ByteRegister scx,
                        @Scy ByteRegister scy,
                        @Wx ByteRegister wx,
                        @Wy ByteRegister wy,
                        @Lcdc ByteRegister lcdc,
                        @Stat ByteRegister stat,
                        @Bgp ByteRegister bgp,
                        @Obp0 ByteRegister obp0,
                        @Obp1 ByteRegister obp1) {
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

        // Keep reference to StatRegister for PPU internal writes
        this.statRegister = (stat instanceof StatRegister) ? (StatRegister) stat : null;
    }

    public byte read(PpuRegister register) {
        return registerMap.get(register).read();
    }

    public void write(PpuRegister register, byte value) {
        registerMap.get(register).write(value);
    }

    /**
     * Set PPU mode bits (0-1) in STAT register. For internal PPU use only.
     */
    public void setStatMode(StatParser.PpuMode mode) {
        if (statRegister != null) {
            statRegister.setMode(mode);
        }
    }

    /**
     * Set coincidence flag (bit 2) in STAT register. For internal PPU use only.
     */
    public void setStatCoincidenceFlag(boolean flag) {
        if (statRegister != null) {
            statRegister.setCoincidenceFlag(flag);
        }
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
