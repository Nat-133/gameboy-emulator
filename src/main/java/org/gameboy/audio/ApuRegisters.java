package org.gameboy.audio;

import org.gameboy.common.ByteRegister;
import java.util.HashMap;
import java.util.Map;

public class ApuRegisters {
    public final ApuRegister nr10 = new ApuRegister(0x7F);
    public final ApuRegister nr11 = new ApuRegister(0xC0);
    public final ApuRegister nr12 = new ApuRegister(0xFF);
    public final ApuRegister nr13 = new ApuRegister(0x00);
    public final ApuRegister nr14 = new ApuRegister(0x40);

    public final ApuRegister nr21 = new ApuRegister(0xC0);
    public final ApuRegister nr22 = new ApuRegister(0xFF);
    public final ApuRegister nr23 = new ApuRegister(0x00);
    public final ApuRegister nr24 = new ApuRegister(0x40);

    public final ApuRegister nr30 = new ApuRegister(0x80);
    public final ApuRegister nr31 = new ApuRegister(0x00);
    public final ApuRegister nr32 = new ApuRegister(0x60);
    public final ApuRegister nr33 = new ApuRegister(0x00);
    public final ApuRegister nr34 = new ApuRegister(0x40);

    public final ApuRegister nr41 = new ApuRegister(0x00);
    public final ApuRegister nr42 = new ApuRegister(0xFF);
    public final ApuRegister nr43 = new ApuRegister(0xFF);
    public final ApuRegister nr44 = new ApuRegister(0x40);

    public final ApuRegister nr50 = new ApuRegister(0xFF);
    public final ApuRegister nr51 = new ApuRegister(0xFF);
    public final ApuRegister nr52 = new ApuRegister(0x8F);

    private final byte[] waveRam = new byte[16];
    private final Map<Integer, ByteRegister> registerMap = new HashMap<>();

    public ApuRegisters() {
        registerMap.put(0xFF10, nr10);
        registerMap.put(0xFF11, nr11);
        registerMap.put(0xFF12, nr12);
        registerMap.put(0xFF13, nr13);
        registerMap.put(0xFF14, nr14);
        registerMap.put(0xFF16, nr21);
        registerMap.put(0xFF17, nr22);
        registerMap.put(0xFF18, nr23);
        registerMap.put(0xFF19, nr24);
        registerMap.put(0xFF1A, nr30);
        registerMap.put(0xFF1B, nr31);
        registerMap.put(0xFF1C, nr32);
        registerMap.put(0xFF1D, nr33);
        registerMap.put(0xFF1E, nr34);
        registerMap.put(0xFF20, nr41);
        registerMap.put(0xFF21, nr42);
        registerMap.put(0xFF22, nr43);
        registerMap.put(0xFF23, nr44);
        registerMap.put(0xFF24, nr50);
        registerMap.put(0xFF25, nr51);
        registerMap.put(0xFF26, nr52);

        for (int i = 0; i < 16; i++) {
            final int offset = i;
            registerMap.put(0xFF30 + i, new ByteRegister() {
                @Override public byte read() { return waveRam[offset]; }
                @Override public void write(byte value) { waveRam[offset] = value; }
            });
        }
    }

    public Map<Integer, ByteRegister> getRegisterMap() { return registerMap; }
    public byte[] getWaveRam() { return waveRam; }
}
