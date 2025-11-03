package org.gameboy.display;

import org.gameboy.common.Clock;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.Optional;

import static org.gameboy.display.Display.DISPLAY_WIDTH;
import static org.gameboy.display.LcdcParser.windowDisplayEnabled;
import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.gameboy.utils.BitUtilities.mod;
import static org.gameboy.utils.BitUtilities.uint;

public class ScanlineController {
    private final Clock ppuClock;
    private final Display display;
    private final Fifo<TwoBitValue> backgroundFifo;
    private final Fifo<SpritePixel> spriteFifo;
    private final PixelCombinator pixelCombinator;
    private final PpuRegisters registers;
    private final BackgroundFetcher backgroundFetcher;
    private final SpriteFetcher spriteFetcher;
    private final SpriteBuffer spriteBuffer;
    private State state;

    private int LX;

    public ScanlineController(Clock ppuClock,
                              Display display,
                              Fifo<TwoBitValue> backgroundFifo,
                              Fifo<SpritePixel> spriteFifo,
                              PixelCombinator pixelCombinator,
                              PpuRegisters registers,
                              BackgroundFetcher backgroundFetcher,
                              SpriteFetcher spriteFetcher,
                              SpriteBuffer spriteBuffer) {
        this.ppuClock = ppuClock;
        this.display = display;
        this.backgroundFifo = backgroundFifo;
        this.spriteFifo = spriteFifo;
        this.pixelCombinator = pixelCombinator;
        this.registers = registers;
        this.backgroundFetcher = backgroundFetcher;
        this.spriteFetcher = spriteFetcher;
        this.spriteBuffer = spriteBuffer;

        LX = 0;

        this.state = State.DISCARD_PIXELS;
    }

    public void performSingleClockCycle() {
        this.state = switch (this.state) {
            case DISCARD_PIXELS -> discardPixel();
            case PIXEL_FETCHING -> executeFetch();
            case SPRITE_FETCHING -> spriteFetch();
            case COMPLETE -> nop();
        };
    }

    public boolean drawingComplete() {
        return state == State.COMPLETE;
    }

    private State nop() {
        ppuClock.tick();
        return State.COMPLETE;
    }

    public void setupScanline() {
        LX = 0;
        backgroundFetcher.reset();
        state = shouldDiscardPixel() ? State.DISCARD_PIXELS : State.PIXEL_FETCHING;
    }

    public void resetForNewFrame() {
        backgroundFetcher.resetForNewFrame();
    }

    private State discardPixel() {
        backgroundFetcher.runSingleTickCycle();
        backgroundFifo.read();

        ppuClock.tick();

        return shouldDiscardPixel() ? State.DISCARD_PIXELS : State.PIXEL_FETCHING;
    }

    private State executeFetch() {
        if (LX >= DISPLAY_WIDTH) {
            return nop();
        }
        if (shouldPerformSpriteFetch(LX)) {
            spriteFetcher.setupFetch(LX);
            return spriteFetch();
        }

        pushPixel();

        return State.PIXEL_FETCHING;
    }

    private void pushPixel() {
        backgroundFetcher.runSingleTickCycle();
        Optional<CombinedPixel> pixelData = getPixelDataWithSprite();

        if (pixelData.isPresent()) {
            CombinedPixel combined = pixelData.get();
            PixelValue pixel = combined.spritePixel() != null
                    ? pixelCombinator.combinePixels(combined.backgroundValue(), combined.spritePixel())
                    : pixelCombinator.combinePixels(combined.backgroundValue());
            display.setPixel(LX, uint(registers.read(LY)), pixel);

            LX++;

            if (atWindow()) {
                backgroundFetcher.switchToWindowFetching();
            }
        }

        ppuClock.tick();
    }

    private boolean atWindow() {
        return (windowDisplayEnabled(registers.read(LCDC)))
                && ((uint(registers.read(WY)) <= uint(registers.read(LY))))
                && (LX >= uint(registers.read(WX)) - 7);
    }

    private State spriteFetch() {
        spriteFetcher.runSingleTickCycle();
        return spriteFetcher.fetchComplete() ? State.PIXEL_FETCHING : State.SPRITE_FETCHING;
    }

    private boolean shouldPerformSpriteFetch(int x) {
        return LcdcParser.objectEnable(registers.read(LCDC)) && spriteBuffer.getSprite(x).isPresent();
    }

    private Optional<CombinedPixel> getPixelDataWithSprite() {
        Optional<TwoBitValue> backgroundData = backgroundFifo.read();

        if (backgroundData.isEmpty()) {
            return Optional.empty();
        }

        TwoBitValue bgValue = backgroundData.get();
        if (!LcdcParser.backgroundAndWindowEnable(registers.read(LCDC))) {
            bgValue = TwoBitValue.b00;
        }

        Optional<SpritePixel> spriteData = spriteFifo.read();

        return Optional.of(new CombinedPixel(bgValue, spriteData.orElse(null)));
    }

    private boolean shouldDiscardPixel() {
        int pixelsToDiscard = mod(registers.read(SCX), 8);
        int discardedPixels = (8 - backgroundFifo.size());
        return discardedPixels % 8 != pixelsToDiscard;
    }

    private enum State {
        DISCARD_PIXELS,
        PIXEL_FETCHING,
        SPRITE_FETCHING,
        COMPLETE
    }

    public record CombinedPixel(
            TwoBitValue backgroundValue,
            SpritePixel spritePixel  // may be null if no sprite at this position
    ) {
    }
}
