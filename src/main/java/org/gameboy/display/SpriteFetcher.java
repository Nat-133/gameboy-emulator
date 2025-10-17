package org.gameboy.display;

import org.gameboy.common.Memory;
import org.gameboy.common.SynchronisedClock;
import org.gameboy.utils.BitUtilities;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.List;
import java.util.stream.IntStream;

import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.utils.BitUtilities.uint;

public class SpriteFetcher implements Fetcher {
    private static final SpriteData EMPTY_SPRITE = new SpriteData((byte) 0, (byte) 0, (byte) 0, (byte) 0);

    private final SpriteBuffer spriteBuffer;
    private final Memory memory;
    private final PpuRegisters registers;
    private final PixelFifo spriteFifo;
    private int pixelXPosition;
    // tile coordinate, not pixel
    private byte tileDataLow;
    private byte tileDataHigh;
    private final SynchronisedClock clock;

    private Step currentStep;
    private SpriteData currentSpriteData;

    public SpriteFetcher(SpriteBuffer spriteBuffer,
                         Memory memory,
                         PpuRegisters registers,
                         PixelFifo spriteFifo,
                         SynchronisedClock clock) {
        this.spriteBuffer = spriteBuffer;
        this.memory = memory;
        this.registers = registers;
        this.spriteFifo = spriteFifo;
        this.clock = clock;
        this.currentStep = Step.FETCH_TILE_NO;
        this.pixelXPosition = 0;
    }

    public void runSingleTickCycle() {
        currentStep = runStep(currentStep);
    }

    public void setupFetch(int x) {
        pixelXPosition = x;
        currentStep = Step.FETCH_TILE_NO;
    }

    public boolean fetchComplete() {
        return currentStep == Step.COMPLETE;
    }

    private Step runStep(Step step) {
        return switch (step) {
            case FETCH_TILE_NO -> fetchTileNo();
            case FETCH_TILE_DATA_LOW -> fetchTileDataLow();
            case FETCH_TILE_DATA_HIGH -> fetchTileDataHigh();
            case PUSH_TO_FIFO -> pushToFifo();
            case TICK_FETCH_TILE_DATA_LOW,
                 TICK_FETCH_TILE_DATA_HIGH -> extraTick(step);
            case COMPLETE -> Step.COMPLETE;
        };
    }

    private Step fetchTileNo() {
        currentSpriteData = spriteBuffer.popSprite(pixelXPosition).orElse(EMPTY_SPRITE);

        clock.tick();
        return Step.FETCH_TILE_NO.next();
    }

    private Step fetchTileDataLow() {
        int tileDataAddress = getTileNumberAddress(uint(currentSpriteData.tileNumber()));
        int rowDataAddress = getTileRow(tileDataAddress, uint(registers.read(LY)), uint(currentSpriteData.y()));
        tileDataLow = memory.read((short) rowDataAddress);
        clock.tick();
        return Step.FETCH_TILE_DATA_LOW.next();
    }

    private Step fetchTileDataHigh() {
        int tileDataAddress = getTileNumberAddress(uint(currentSpriteData.tileNumber()));
        int rowDataAddress = 1 + getTileRow(tileDataAddress, uint(registers.read(LY)), uint(currentSpriteData.y()));
        tileDataHigh = memory.read((short) rowDataAddress);
        clock.tick();
        return Step.FETCH_TILE_DATA_HIGH.next();
    }

    private Step pushToFifo() {
        List<TwoBitValue> pixelData = IntStream.range(0, 8)
                .map(i -> !currentSpriteData.xFlipFlag() ? i : 7-i)
                .map(i -> (BitUtilities.get_bit(tileDataLow, i) ? 1 : 0)
                        + (BitUtilities.get_bit(tileDataHigh, i) ? 2 : 0))
                .mapToObj(TwoBitValue::from)
                .toList();
        spriteFifo.write(pixelData);
        clock.tick();

        return Step.COMPLETE;
    }

    private Step extraTick(Step step) {
        clock.tick();
        return step.next();
    }

    private int getTileNumberAddress(int tileNumber) {
        int spriteHeight = LcdcParser.spriteSize(registers.read(PpuRegisters.PpuRegister.LCDC));
        tileNumber = spriteHeight == 16 ? tileNumber & 0xFE : tileNumber;
        return 0x8000 + ((tileNumber) * 16);
    }

    private int getTileRow(int tileDataAddress, int ly, int spriteY) {
        int spriteHeight = LcdcParser.spriteSize(registers.read(PpuRegisters.PpuRegister.LCDC));
        int spriteRow = (ly + 16) - spriteY;

        if (currentSpriteData.yFlipFlag()) {
            spriteRow = (spriteHeight - 1) - spriteRow;
        }

        return tileDataAddress + 2 * (spriteRow);
    }

    private enum Step {
        FETCH_TILE_NO,

        FETCH_TILE_DATA_LOW,
        TICK_FETCH_TILE_DATA_LOW,

        FETCH_TILE_DATA_HIGH,
        TICK_FETCH_TILE_DATA_HIGH,

        PUSH_TO_FIFO,
        COMPLETE;

        public Step next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }
}
