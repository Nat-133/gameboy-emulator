package org.gameboy.display;

import org.gameboy.common.Clock;

import static org.gameboy.display.PpuRegisters.PpuRegister.LCDC;
import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.utils.BitUtilities.uint;

public class PictureProcessingUnit {

    public static final int SCANLINE_TICK_COUNT = 456;
    private final ScanlineController scanlineController;
    private final PpuRegisters registers;
    private final Clock clock;
    private final OamScanController oamScanController;
    private final DisplayInterruptController displayInterruptController;
    private final Display display;
    private int count = 0;
    private Step step;
    private boolean wasLcdEnabled = true;

    public PictureProcessingUnit(ScanlineController scanlineController,
                                 PpuRegisters registers,
                                 Clock clock,
                                 OamScanController oamScanController,
                                 DisplayInterruptController displayInterruptController,
                                 Display display) {
        this.scanlineController = scanlineController;
        this.registers = registers;
        this.clock = clock;
        this.oamScanController = oamScanController;
        this.displayInterruptController = displayInterruptController;
        this.display = display;
        this.step = Step.OAM_SETUP;
    }

    public void tCycle() {
        boolean lcdEnabled = LcdcParser.lcdEnabled(registers.read(LCDC));

        // Handle LCD disabled state
        if (!lcdEnabled) {
            if (wasLcdEnabled) {
                // Just got disabled - set mode to HBlank but don't reset LY
                // The coincidence flag is retained
                registers.setStatMode(StatParser.PpuMode.H_BLANK);
                wasLcdEnabled = false;
            }
            return;  // PPU does not run when LCD is disabled
        }

        // LCD just got enabled - reset PPU state
        if (!wasLcdEnabled) {
            wasLcdEnabled = true;
            registers.write(LY, (byte) 0);
            count = 0;
            step = Step.OAM_SETUP;
            // When LCD is enabled, check LY/LYC coincidence immediately
            displayInterruptController.checkAndSendLyCoincidence();
            return;  // Start fresh on next tCycle
        }

        step = switch(step) {
            case OAM_SETUP -> setupOamScan();
            case OAM_SCAN -> oamScan();
            case SCANLINE_SETUP -> setupScanline();
            case SCANLINE_DRAWING -> drawScanline();
            case HBLANK -> hblank();
            case VBLANK -> vblank();
        };
    }

    private Step setupOamScan() {
        displayInterruptController.sendOamScan();
        oamScanController.setupOamScan(uint(registers.read(LY)));
        count = 0;
        return oamScan();
    }

    private Step oamScan() {
        oamScanController.performOneClockCycle();
        count++;
        return count < 40*2 ? Step.OAM_SCAN : Step.SCANLINE_SETUP;
    }

    private Step setupScanline() {
        displayInterruptController.sendDrawing();
        scanlineController.setupScanline();
        count = 0;
        return drawScanline();
    }

    private Step drawScanline() {
        scanlineController.performSingleClockCycle();
        count++;

        if (!scanlineController.drawingComplete()) {
            return Step.SCANLINE_DRAWING;
        }

        displayInterruptController.sendHblank();
        return Step.HBLANK;
    }

    private Step hblank() {
        clock.tick();
        count++;

        if (count < SCANLINE_TICK_COUNT) {
            return Step.HBLANK;
        }

        updateLY((byte) (registers.read(LY) + 1));

        if (uint(registers.read(LY)) >= Display.DISPLAY_HEIGHT) {
            count = 0;
            displayInterruptController.sendVblank();
            display.onVBlank();
            return Step.VBLANK;
        }

        return Step.OAM_SETUP;
    }

    private Step vblank() {
        clock.tick();
        count++;
        
        // Increment LY every scanline during VBLANK
        if (count % SCANLINE_TICK_COUNT == 0) {
            int currentLy = uint(registers.read(LY));
            if (currentLy < 153) {
                updateLY((byte) (currentLy + 1));
            }
        }
        
        if (count < SCANLINE_TICK_COUNT * 10) {
            return Step.VBLANK;
        }

        updateLY((byte) 0);
        scanlineController.resetForNewFrame();
        return Step.OAM_SETUP;
    }

    private void updateLY(byte value) {
        registers.write(LY, value);
        displayInterruptController.checkAndSendLyCoincidence();
    }

    private enum Step {
        OAM_SETUP,
        OAM_SCAN,
        SCANLINE_SETUP,
        SCANLINE_DRAWING,
        HBLANK,
        VBLANK
    }
}
