package org.gameboy.display;

import org.gameboy.common.Clock;

import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.utils.BitUtilities.uint;

public class PictureProcessingUnit {

    private final ScanlineController scanlineController;
    private final PpuRegisters registers;
    private final ObjectAttributeMemory oam;
    private final Clock clock;
    private final SpriteBuffer spriteBuffer;
    private final InterruptController interruptController;
    private int count = 0;

    public PictureProcessingUnit(ScanlineController scanlineController,
                                 PpuRegisters registers,
                                 ObjectAttributeMemory oam,
                                 Clock clock,
                                 SpriteBuffer spriteBuffer,
                                 InterruptController interruptController) {
        this.scanlineController = scanlineController;
        this.registers = registers;
        this.oam = oam;
        this.clock = clock;
        this.spriteBuffer = spriteBuffer;
        this.interruptController = interruptController;
    }

    public void renderAllScanlines() {
        for (int y = 0; y < 144; y++) {
            // ly=wy interrupt still needs to happen
            // or was that x = wx interrupt?
            // no, I'm pretty sure it was the y one.
            registers.write(LY, (byte) y);
            if (y == uint(registers.read(PpuRegisters.PpuRegister.SCY))) {
                interruptController.sendScanlineAtWindow();
            }

            spriteBuffer.scanOAM(oam, uint(registers.read(LY)), clock);
//            scanlineController.renderScanline(y);
            for (int i=0; i<173; i++) scanlineController.performSingleClockCycle();

            scanlineController.reset();

            interruptController.sendHBLANK();
            performHblankPadding();
        }

        // send vblank interrupt
        interruptController.sendVBLANK();
        performVblankPadding();

        // do nothing for a few scanlines worth of time

        byte read = registers.read(PpuRegisters.PpuRegister.SCX);
//        if (count++ % 10 == 0) registers.write(PpuRegisters.PpuRegister.SCX, (byte)(read + 1));
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

    // scanline controller:
    //  OAM scannin'
    //  x register
    //  fetcher pausing/playing
    //  Sprite detection -> switching fetch modes
    //  HBlank padding
    //
    // PPU:
    //  VBlank padding
    //  y register
    //  triggering the scanline controller


    // tests
    // givenSpritesAtY_whenOamScanned_thenCorrectSpritesFound
    // givenNoSpritesAtY_whenOamScanned_thenCorrectSpritesFound
    //
    // givenSevenSpritesInBuffer_whenScanlineDrawn_thenTakesNTicksToDraw
    // givenSevenSpritesInBuffer_whenScanlineDrawn_thenHBlankIsNTicks
    //
    // givenSpriteAtCurrentX_whenAdvance_thenBackgroundFetcherCorrectlyPaused
    // givenSpriteFetchOccurring_whenItFinishes_thenBackgroundFetcherIsResumed
    // givenTwoOverlappingSprites_whenSecondFetchOccurs_thenFirstSpriteTakesPrecedent
    //
    // 144 scanlines drawn + hblank scanlines
    // total screen refresh takes some time, for various sprites
    // correct interrupts sent
    //


    /*
     *separate components:
     *  OAM scanner
     *  fetcher
     *      background
     *      sprite
     *  drawing to screen part of the scanline
     *  hblank mode part of scanline
     *  vblank part of drawing screen
     */
}
