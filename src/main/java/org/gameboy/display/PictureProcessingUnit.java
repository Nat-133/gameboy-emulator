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
    private final InterruptController interruptController;
    private int count = 0;
    private Step step;

    public PictureProcessingUnit(ScanlineController scanlineController,
                                 PpuRegisters registers,
                                 Clock clock,
                                 OamScanController oamScanController,
                                 InterruptController interruptController) {
        this.scanlineController = scanlineController;
        this.registers = registers;
        this.clock = clock;
        this.oamScanController = oamScanController;
        this.interruptController = interruptController;
        this.step = Step.OAM_SETUP;
    }

    public void performOneClockCycle() {
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
        interruptController.sendOamScan();
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

        interruptController.sendHblank();
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
            interruptController.sendVblank();
            return Step.VBLANK;
        }

        return Step.OAM_SETUP;
    }

    private Step vblank() {
        clock.tick();
        count++;
        if (count < SCANLINE_TICK_COUNT * 10) {
            return Step.VBLANK;
        }
        // todo: increment LY still

        updateLY((byte) 0);
        return Step.OAM_SETUP;
    }

    private void updateLY(byte value) {
        registers.write(LY, value);
        interruptController.sendLyCoincidence();
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
