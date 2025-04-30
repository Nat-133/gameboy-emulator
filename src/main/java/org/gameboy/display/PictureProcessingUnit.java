package org.gameboy.display;

public class PictureProcessingUnit {

    private final ScanlineController scanlineController;
    private final PpuRegisters registers;

    public PictureProcessingUnit(ScanlineController scanlineController, PpuRegisters registers) {
        this.scanlineController = scanlineController;
        this.registers = registers;
    }

    public void renderAllScanlines() {
        for (int y = 0; y < 144; y++) {
            scanlineController.renderScanline(y);
        }

        byte read = registers.read(PpuRegisters.PpuRegister.SCX);
        registers.write(PpuRegisters.PpuRegister.SCX, (byte)(read +1));
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
