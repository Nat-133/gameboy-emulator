package org.gameboy.display;

import org.gameboy.common.Interrupt;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryMapConstants;

import static org.gameboy.utils.BitUtilities.set_bit;

public class InterruptController {
    private final Memory memory;
    private final PpuRegisters registers;
    private final StatParser statParser;

    public InterruptController(Memory memory, PpuRegisters registers, StatParser statParser) {
        this.memory = memory;
        this.registers = registers;
        this.statParser = statParser;
    }

    public void sendHBLANK() {
        byte stat = registers.read(PpuRegisters.PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.H_BLANK, stat);

        if (StatParser.hblankInterruptEnabled(stat)) {
            setInterrupt(Interrupt.STAT);
        }
    }

    public void sendLyCoincidence() {
        byte stat = registers.read(PpuRegisters.PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.H_BLANK, stat);

        if (StatParser.lyCompareInterruptEnabled(stat)) {
            setInterrupt(Interrupt.STAT);
        }
    }

    public void sendVblank() {

        byte stat = registers.read(PpuRegisters.PpuRegister.STAT);
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
