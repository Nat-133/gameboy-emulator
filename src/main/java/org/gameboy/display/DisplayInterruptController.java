package org.gameboy.display;

import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.gameboy.display.PpuRegisters.PpuRegister;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DisplayInterruptController {
    private final InterruptController interruptController;
    private final PpuRegisters registers;
    
    private Optional<ActiveModeLine> activeModeLine = Optional.empty();
    private boolean lycLine = false;
    private boolean statLine = false;
    
    private enum ActiveModeLine {
        HBLANK,
        VBLANK,
        OAM
    }

    public DisplayInterruptController(InterruptController interruptController, PpuRegisters registers) {
        this.interruptController = interruptController;
        this.registers = registers;
    }

    public void sendHblank() {
        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.H_BLANK, stat);
        registers.write(PpuRegister.STAT, stat);

        activeModeLine = StatParser.hblankInterruptEnabled(stat)
                ? Optional.of(ActiveModeLine.HBLANK)
                : Optional.empty();
        
        checkAndTriggerStatInterrupt();
    }

    public void sendOamScan() {
        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.OAM_SCANNING, stat);
        registers.write(PpuRegister.STAT, stat);

        activeModeLine = StatParser.oamInterruptEnabled(stat)
                ? Optional.of(ActiveModeLine.OAM)
                : Optional.empty();

        checkAndTriggerStatInterrupt();
    }

    public void sendLyCoincidence() {
        byte stat = registers.read(PpuRegister.STAT);
        boolean lyIsLyc = registers.read(PpuRegister.LY) == registers.read(PpuRegister.LYC);

        stat = StatParser.setCoincidenceFlag(stat, lyIsLyc);
        registers.write(PpuRegister.STAT, stat);
        
        lycLine = lyIsLyc && StatParser.lyCompareInterruptEnabled(stat);
        
        checkAndTriggerStatInterrupt();
    }

    public void sendVblank() {

        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.V_BLANK, stat);
        registers.write(PpuRegister.STAT, stat);

        interruptController.setInterrupt(Interrupt.VBLANK);

        activeModeLine = StatParser.vblankStatInterruptEnabled(stat)
                ? Optional.of(ActiveModeLine.VBLANK)
                : Optional.empty();
        
        checkAndTriggerStatInterrupt();
    }

    public void sendDrawing() {
        byte stat = registers.read(PpuRegister.STAT);
        stat = StatParser.setPpuMode(StatParser.PpuMode.DRAWING, stat);
        registers.write(PpuRegister.STAT, stat);
    }

    private void checkAndTriggerStatInterrupt() {
        boolean newStatLine = activeModeLine.isPresent() || lycLine;
        
        if (newStatLine && !statLine) {  // this is for stat blocking, only trigger on rising edge
            interruptController.setInterrupt(Interrupt.STAT);
        }
        
        statLine = newStatLine;
    }
}
