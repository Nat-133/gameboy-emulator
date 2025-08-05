package org.gameboy.display;

import org.gameboy.common.Clock;

import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.utils.BitUtilities.uint;

public class PictureProcessingUnit {

    public static final int SCANLINE_TICK_COUNT = 456;
    private final ScanlineController scanlineController;
    private final PpuRegisters registers;
    private final Clock clock;
    private final OamScanController oamScanController;
    private final DisplayInterruptController displayInterruptController;
    private int count = 0;
    private Step step;

    public PictureProcessingUnit(ScanlineController scanlineController,
                                 PpuRegisters registers,
                                 Clock clock,
                                 OamScanController oamScanController,
                                 DisplayInterruptController displayInterruptController) {
        this.scanlineController = scanlineController;
        this.registers = registers;
        this.clock = clock;
        this.oamScanController = oamScanController;
        this.displayInterruptController = displayInterruptController;
        this.step = Step.OAM_SETUP;
    }

    public void tCycle() {
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
        return Step.OAM_SETUP;
    }

    private void updateLY(byte value) {
        registers.write(LY, value);
        displayInterruptController.sendLyCoincidence();
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
