package org.gameboy.display;

import org.gameboy.common.Memory;
import org.gameboy.common.MemoryMapConstants;
import org.gameboy.common.SynchronisedClock;
import org.gameboy.utils.BitUtilities;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.List;
import java.util.stream.IntStream;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.gameboy.utils.BitUtilities.uint;

public class BackgroundFetcher implements Fetcher {
    private final Memory memory;
    private final PpuRegisters registers;
    private final Fifo<TwoBitValue> backgroundFifo;
    private int X_POSITION_COUNTER = 0;  // tile coordinate, not pixel
    private byte currentTileNumber;
    private byte tileDataLow;
    private byte tileDataHigh;
    private final SynchronisedClock clock;

    private Step currentStep;
    private boolean windowFetchMode;

    public BackgroundFetcher(Memory memory,
                             PpuRegisters registers,
                             Fifo<TwoBitValue> backgroundFifo,
                             SynchronisedClock clock) {
        this.memory = memory;
        this.registers = registers;
        this.backgroundFifo = backgroundFifo;
        this.clock = clock;

        this.currentStep = Step.FETCH_TILE_NO;
        this.windowFetchMode = false;
    }

    private int getTilemapIndex(int x, int y, int background_offset_x, int background_offset_y) {
        return ((x+((background_offset_x) / 8)) & 0x1f) + 32 * (((y + background_offset_y) & 0xff) / 8);
    }

    private int getSignedTileNumberAddress(int tileNumber) {
        return MemoryMapConstants.TILE_DATA_ADDRESS + 0x1000 + (((byte)tileNumber) * 16);
    }

    private int getUnsignedTileNumberAddress(int tileNumber) {
        return MemoryMapConstants.TILE_DATA_ADDRESS + (tileNumber * 16);
    }

    private int getTileDataAddress(int tileNumber) {
        byte lcdc = registers.read(LCDC);
        boolean unsignedMode = LcdcParser.useUnsignedTileDataSelect(lcdc);

        if (unsignedMode) {
            return getUnsignedTileNumberAddress(tileNumber);
        } else {
            return getSignedTileNumberAddress(tileNumber);
        }
    }

    private int getTileRow(int tileDataAddress, int ly, int scy) {
        return tileDataAddress + 2 * ((ly + scy) % 8);
    }

    @Override
    public void runSingleTickCycle() {
        currentStep = runStep(currentStep);
    }

    public void reset() {
        backgroundFifo.clear();
        currentStep = Step.FETCH_TILE_NO;
        X_POSITION_COUNTER = 0;
        windowFetchMode = false;
    }

    public void switchToWindowFetching() {
        if (!windowFetchMode) {
            reset();
            windowFetchMode = true;
        }
    }

    private Step runStep(Step step) {
        return switch (step) {
            case FETCH_TILE_NO -> fetchTileNo();
            case FETCH_TILE_DATA_LOW -> fetchTileDataLow();
            case FETCH_TILE_DATA_HIGH -> fetchTileDataHigh();
            case PUSH_TO_FIFO -> pushToFifo();
            case TICK_FETCH_TILE_NO,
                 TICK_FETCH_TILE_DATA_LOW,
                 TICK_FETCH_TILE_DATA_HIGH -> extraTick(step);
        };
    }

    private Step fetchTileNo() {
        int tileIndex = getTilemapIndex(X_POSITION_COUNTER, uint(registers.read(LY)), uint(registers.read(SCX)), uint(registers.read(SCY)));
        byte lcdc = registers.read(LCDC);
        int tilemapAddress = windowFetchMode
            ? LcdcParser.windowTileMap(lcdc)
            : LcdcParser.backgroundTileMap(lcdc);

        short tileNumberAddress = (short) (tilemapAddress + tileIndex);

        currentTileNumber = memory.read(tileNumberAddress);

        clock.tick();
        return Step.FETCH_TILE_NO.next();
    }

    private Step fetchTileDataLow() {
        int tileDataAddress = getTileDataAddress(uint(currentTileNumber));
        int rowDataAddress = getTileRow(tileDataAddress, uint(registers.read(LY)), uint(registers.read(SCY)));
        tileDataLow = memory.read((short) rowDataAddress);
        clock.tick();
        return Step.FETCH_TILE_DATA_LOW.next();
    }

    private Step fetchTileDataHigh() {
        int tileDataAddress = getTileDataAddress(uint(currentTileNumber));
        int rowDataAddress = 1 + getTileRow(tileDataAddress, uint(registers.read(LY)), uint(registers.read(SCY)));
        tileDataHigh = memory.read((short) rowDataAddress);
        clock.tick();
        return Step.FETCH_TILE_DATA_HIGH.next();
    }

    private Step pushToFifo() {
        if (!backgroundFifo.isEmpty()) {
            clock.tick();
            return Step.PUSH_TO_FIFO;
        }

        List<TwoBitValue> pixelData = IntStream.range(0, 8).map(i -> 7-i)
                .map(i -> (BitUtilities.get_bit(tileDataLow, i) ? 1 : 0)
                        + (BitUtilities.get_bit(tileDataHigh, i) ? 2 : 0))
                .mapToObj(TwoBitValue::from)
                .toList();
        backgroundFifo.write(pixelData);
        clock.tick();
        X_POSITION_COUNTER++;

        return Step.FETCH_TILE_NO;
    }

    private Step extraTick(Step step) {
        clock.tick();
        return step.next();
    }

    private enum Step {
        FETCH_TILE_NO,
        TICK_FETCH_TILE_NO,

        FETCH_TILE_DATA_LOW,
        TICK_FETCH_TILE_DATA_LOW,

        FETCH_TILE_DATA_HIGH,
        TICK_FETCH_TILE_DATA_HIGH,

        PUSH_TO_FIFO;

        public Step next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }
}
