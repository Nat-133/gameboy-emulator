package org.gameboy.display;

import org.gameboy.common.Interrupt;
import org.gameboy.common.InterruptController;
import org.gameboy.display.PpuRegisters.PpuRegister;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

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
        registers.setStatMode(StatParser.PpuMode.H_BLANK);

        byte stat = registers.read(PpuRegister.STAT);
        activeModeLine = StatParser.hblankInterruptEnabled(stat)
                ? Optional.of(ActiveModeLine.HBLANK)
                : Optional.empty();

        checkAndTriggerStatInterrupt();
    }

    public void sendOamScan() {
        registers.setStatMode(StatParser.PpuMode.OAM_SCANNING);

        byte stat = registers.read(PpuRegister.STAT);
        activeModeLine = StatParser.oamInterruptEnabled(stat)
                ? Optional.of(ActiveModeLine.OAM)
                : Optional.empty();

        checkAndTriggerStatInterrupt();
    }

    public void checkAndSendLyCoincidence() {
        // When LCD is disabled, the LY/LYC comparison clock is not running.
        // The coincidence flag should be retained as-is and no interrupt should fire.
        if (!LcdcParser.lcdEnabled(registers.read(PpuRegister.LCDC))) {
            return;
        }

        boolean lyIsLyc = registers.read(PpuRegister.LY) == registers.read(PpuRegister.LYC);

        // Set coincidence flag using internal PPU method
        registers.setStatCoincidenceFlag(lyIsLyc);

        byte stat = registers.read(PpuRegister.STAT);
        lycLine = lyIsLyc && StatParser.lyCompareInterruptEnabled(stat);

        checkAndTriggerStatInterrupt();
    }

    public void sendVblank() {
        registers.setStatMode(StatParser.PpuMode.V_BLANK);

        interruptController.setInterrupt(Interrupt.VBLANK);

        byte stat = registers.read(PpuRegister.STAT);
        // VBlank STAT interrupt can be triggered by either:
        // - Bit 4: VBlank mode interrupt enable
        // - Bit 5: OAM mode interrupt enable (triggers at start of ANY scanline, including line 144)
        boolean vblankStatEnabled = StatParser.vblankStatInterruptEnabled(stat);
        boolean oamStatEnabled = StatParser.oamInterruptEnabled(stat);

        activeModeLine = (vblankStatEnabled || oamStatEnabled)
                ? Optional.of(ActiveModeLine.VBLANK)
                : Optional.empty();

        checkAndTriggerStatInterrupt();
    }

    public void sendDrawing() {
        registers.setStatMode(StatParser.PpuMode.DRAWING);

        // Mode 3 (Drawing) has no STAT interrupt, so clear the active mode line.
        // This allows the internal STAT IRQ signal to go low (if LYC line is also low),
        // enabling new STAT interrupts when the next mode with an enabled interrupt begins.
        activeModeLine = Optional.empty();

        // Update the internal stat line state (important for STAT blocking)
        updateStatLine();
    }

    public void checkStatCondition() {
        // When LCD is disabled, don't process STAT changes for interrupts
        if (!LcdcParser.lcdEnabled(registers.read(PpuRegister.LCDC))) {
            return;
        }

        byte stat = registers.read(PpuRegister.STAT);
        TwoBitValue mode = TwoBitValue.from(stat, 0);

        // Update active mode line based on current mode and newly written enable bits
        activeModeLine = switch (mode) {
            case b00 -> StatParser.hblankInterruptEnabled(stat) ? Optional.of(ActiveModeLine.HBLANK) : Optional.empty();
            case b01 -> (StatParser.vblankStatInterruptEnabled(stat) || StatParser.oamInterruptEnabled(stat))
                    ? Optional.of(ActiveModeLine.VBLANK) : Optional.empty();
            case b10 -> StatParser.oamInterruptEnabled(stat) ? Optional.of(ActiveModeLine.OAM) : Optional.empty();
            case b11 -> Optional.empty();  // Mode 3 never has an active mode line
        };

        // Also re-check LYC condition with potentially new LYC interrupt enable bit
        boolean lyIsLyc = registers.read(PpuRegister.LY) == registers.read(PpuRegister.LYC);
        lycLine = lyIsLyc && StatParser.lyCompareInterruptEnabled(stat);

        checkAndTriggerStatInterrupt();
    }

    private void updateStatLine() {
        statLine = activeModeLine.isPresent() || lycLine;
    }

    private void checkAndTriggerStatInterrupt() {
        boolean newStatLine = activeModeLine.isPresent() || lycLine;
        
        if (newStatLine && !statLine) {  // this is for stat blocking, only trigger on rising edge
            interruptController.setInterrupt(Interrupt.STAT);
        }
        
        statLine = newStatLine;
    }
}
