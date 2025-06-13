package org.gameboy.display;

import org.gameboy.common.Clock;

import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.utils.BitUtilities.uint;

public class PictureProcessingUnit {

    public static final int SCANLINE_TICK_COUNT = 200;
    private final ScanlineController scanlineController;
    private final PpuRegisters registers;
    private final ObjectAttributeMemory oam;
    private final Clock clock;
    private final SpriteBuffer spriteBuffer;
    private final OamScanner oamScanner;
    private final InterruptController interruptController;
    private int count = 0;
    private Step step;

    public PictureProcessingUnit(ScanlineController scanlineController,
                                 PpuRegisters registers,
                                 ObjectAttributeMemory oam,
                                 Clock clock,
                                 SpriteBuffer spriteBuffer,
                                 OamScanner oamScanner,
                                 InterruptController interruptController) {
        this.scanlineController = scanlineController;
        this.registers = registers;
        this.oam = oam;
        this.clock = clock;
        this.spriteBuffer = spriteBuffer;
        this.oamScanner = oamScanner;
        this.interruptController = interruptController;
        this.step = Step.OAM_SETUP;
    }

    public void performOneClockCycle() {
        step = switch(step) {
            case OAM_SETUP -> setupOamScan();
            case OAM_SCAN -> oamScan();
            case SCANLINE_SETUP -> setupScanline();
            case SCANLINE_DRAWING -> drawScanline();
            case VBLANK -> vblank();
        };
    }

    private Step setupOamScan() {
        oamScanner.setupOamScan(uint(registers.read(LY)));
        count = 0;
        return oamScan();
    }

    private Step oamScan() {
        oamScanner.performOneClockCycle();
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

        if (count < SCANLINE_TICK_COUNT) {
            return Step.SCANLINE_DRAWING;
        }

        registers.write(LY, (byte) (registers.read(LY) + 1));

        if (uint(registers.read(LY)) >= Display.DISPLAY_HEIGHT) {
            count = 0;
            return Step.VBLANK;
        }

        return Step.OAM_SETUP;
    }

    private Step vblank() {
        clock.tick();
        count++;
        if (count < SCANLINE_TICK_COUNT * 40) {
            return Step.VBLANK;
        }

        registers.write(LY, (byte) 0);
        return Step.OAM_SETUP;
    }

    private void performHblankPadding() {
        for (int i=0; i<= 160; i++) {
            clock.tick();
        }
    }

    private void performVblankPadding() {
        for (int i=0; i<= 70; i++) {
            performHblankPadding();
        }
    }
    private enum Step {
        OAM_SETUP,
        OAM_SCAN,
        SCANLINE_SETUP,
        SCANLINE_DRAWING,
        VBLANK
    }
}
