package org.gameboy.display;

import org.gameboy.common.Interrupt;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryMapConstants;
import org.gameboy.display.PpuRegisters.PpuRegister;

import static org.gameboy.utils.BitUtilities.set_bit;

public class DisplayInterruptController {
    private final Memory memory;
    private final PpuRegisters registers;

    public DisplayInterruptController(Memory memory, PpuRegisters registers) {
        this.memory = memory;
        this.registers = registers;
    }

    public void sendHblank() {
        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.H_BLANK, stat);

        if (StatParser.hblankInterruptEnabled(stat)) {
            setInterrupt(Interrupt.STAT);
        }
    }

    public void sendOamScan() {
        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.OAM_SCANNING, stat);

        if (StatParser.oamInterruptEnabled(stat)) {
            setInterrupt(Interrupt.STAT);
        }
    }

    public void sendLyCoincidence() {
        byte stat = registers.read(PpuRegister.STAT);
        boolean lyIsLyc = registers.read(PpuRegister.LY) == registers.read(PpuRegister.LYC);

        registers.write(PpuRegister.STAT, StatParser.setCoincidenceFlag(stat, lyIsLyc));
        if (lyIsLyc && StatParser.lyCompareInterruptEnabled(stat)) {
            setInterrupt(Interrupt.STAT);
        }
    }

    public void sendVblank() {

        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.V_BLANK, stat);

        if (StatParser.vblankInterruptEnabled(stat)) {
            setInterrupt(Interrupt.VBLANK);
        }
    }

    private void setInterrupt(Interrupt interrupt) {
        byte interruptFlags = memory.read(MemoryMapConstants.IF_ADDRESS);
        memory.write(MemoryMapConstants.IF_ADDRESS, set_bit(interruptFlags, interrupt.index(), true));
    }
}
