package org.gameboy.display;

import org.gameboy.common.Memory;
import org.gameboy.common.MemoryMapConstants;
import org.gameboy.common.SynchronisedClock;
import org.gameboy.utils.BitUtilities;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.gameboy.utils.BitUtilities.uint;

public class BackgroundFetcher implements Fetcher {
    private final Memory memory;
    private final PpuRegisters registers;
    private final PixelFifo backgroundFifo;
    //    private int WINDOW_LINE_COUNTER = 0;  // should be on fetcher, not background fetcher
    private int X_POSITION_COUNTER = 0;  // tile coordinate, not pixel
    private byte currentTileNumber;
    private byte tileDataLow;
    private byte tileDataHigh;
    private final SynchronisedClock clock;

    public List<List<TwoBitValue>> history = new ArrayList<>();
    private Step currentStep;
    private boolean windowFetchMode;

    public BackgroundFetcher(Memory memory,
                             PpuRegisters registers,
                             PixelFifo backgroundFifo,
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

    private int getTileNumberAddress(int tileNumber) {
        return 0x8000 + (tileNumber * 16);
    }

    private int getSignedTileNumberAddress(int tileNumber) {
        return 0x9000 + (((byte)tileNumber) * 16);
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
        reset();
        windowFetchMode = true;
        // todo: how ss in windowState and then have a full reset method on it
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
        short tileNumberAddress = (short) (MemoryMapConstants.TILE_MAP_A_ADDRESS + tileIndex);

        currentTileNumber = memory.read(tileNumberAddress);

        clock.tick();
        return Step.FETCH_TILE_NO.next();
    }

    private Step fetchTileDataLow() {
        int tileDataAddress = getSignedTileNumberAddress(currentTileNumber);
        int rowDataAddress = getTileRow(tileDataAddress, registers.read(LY), 0);
        tileDataLow = memory.read((short) rowDataAddress);
        clock.tick();
        return Step.FETCH_TILE_DATA_LOW.next();
    }

    private Step fetchTileDataHigh() {
        int tileDataAddress = getSignedTileNumberAddress(currentTileNumber);
        int rowDataAddress = 1 + getTileRow(tileDataAddress, registers.read(LY), 0);
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
