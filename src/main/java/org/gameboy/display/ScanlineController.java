package org.gameboy.display;

import org.gameboy.common.Clock;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.Optional;

import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.display.PpuRegisters.PpuRegister.SCX;
import static org.gameboy.utils.BitUtilities.mod;
import static org.gameboy.utils.BitUtilities.uint;

public class ScanlineController {
    private final Clock ppuClock;
    private final Display display;
    private final PixelFifo backgroundFifo;
    private final PixelCombinator pixelCombinator;
    private final PpuRegisters registers;
    private final BackgroundFetcher backgroundFetcher;

    public ScanlineController(Clock ppuClock,
                              Display display,
                              PixelFifo backgroundFifo,
                              PixelCombinator pixelCombinator,
                              PpuRegisters registers,
                              BackgroundFetcher backgroundFetcher) {
        this.ppuClock = ppuClock;
        this.display = display;
        this.backgroundFifo = backgroundFifo;
        this.pixelCombinator = pixelCombinator;
        this.registers = registers;
        this.backgroundFetcher = backgroundFetcher;
    }

    public void renderScanline(int y) {
        backgroundFetcher.unpause();
        discardOffscreenPixels();

        int x=0;
        while (x<160) {
            ppuClock.tick();
            Optional<TwoBitValue> pixelData = backgroundFifo.read();

            if (pixelData.isPresent()) {
                PixelValue pixel = pixelCombinator.combinePixels(pixelData.get());
                display.setPixel(x, y, pixel);

                x++;
            }

        }

        backgroundFetcher.resetAndPause();  // this needs to block until the dude is actually paused
        registers.write(LY, (byte) ((uint(registers.read(LY)) + 1) % 144));

        for (int i=0; i<10; i++) {
            ppuClock.tick();
        }
    }

    private void discardOffscreenPixels() {
        int x=0;
        while (x < mod(registers.read(SCX), 8)) {
            ppuClock.tick();
            Optional<TwoBitValue> pixelData = backgroundFifo.read();
            if (pixelData.isPresent()) {
                x++;
            }
        }
    }
}
