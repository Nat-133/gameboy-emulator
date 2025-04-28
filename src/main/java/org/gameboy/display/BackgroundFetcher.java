package org.gameboy.display;

import org.gameboy.common.Clock;
import org.gameboy.common.Memory;
import org.gameboy.utils.BitUtilities;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.gameboy.display.PpuRegisters.PpuRegister.LY;
import static org.gameboy.utils.BitUtilities.uint;

public class BackgroundFetcher {
    public static final int BACKGROUND_MAP_A_ADDRESS = 0x9800;
    public static final int BACKGROUND_MAP_B_ADDRESS = 0x9C00;
    private final Memory memory;
    private final PpuRegisters registers;
    private final PixelFifo backgroundFifo;
    //    private int WINDOW_LINE_COUNTER = 0;  // should be on fetcher, not background fetcher
    private int X_POSITION_COUNTER = 0;
    private byte currentTileNumber;
    private byte tileDataLow;
    private byte tileDataHigh;
    private Clock clock;
    private SignalObserver pauseObserver;
    private boolean paused;

    public List<List<TwoBitValue>> history = new ArrayList<>();

    public BackgroundFetcher(Memory memory, PpuRegisters registers, PixelFifo backgroundFifo, Clock clock) {
        this.memory = memory;
        this.registers = registers;
        this.backgroundFifo = backgroundFifo;
        this.clock = clock;
        this.paused = false;
        this.pauseObserver = new SignalObserver();
    }

    private int getTilemapIndex(int x, int y, int background_offset_x, int background_offset_y) {
        return x + (( background_offset_x) / 8 & 0x1f) + 32 * (((y + background_offset_y) & 0xff) / 8);
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

    public void runFetcher() {
        Step currentStep = Step.FETCH_TILE_NO;

        while (true) {
            if (uint(registers.read(LY)) == 0) history.add(new ArrayList<>());
            while (!paused) {
                currentStep = runStep(currentStep);
            }

            pauseAndWait();
//            if (uint(registers.read(LY)) == 0 && history.size() > 1) System.out.println(printList(history.getLast()).equals(printList(history.get(history.size() - 2))));
            currentStep = Step.FETCH_TILE_NO;
            paused = false;
        }
    }

    private void pauseAndWait() {
        backgroundFifo.clear();
        X_POSITION_COUNTER = 0;
        clock.tick();
        pauseObserver.waitForSignal();
        pauseObserver.reset();
    }

    public void resetAndPause() {
        paused = true;
    }

    public void unpause() {
        pauseObserver.signal();
    }

    private Step runStep(Step step) {
        return switch (step) {
            case FETCH_TILE_NO -> fetchTileNo();
            case FETCH_TILE_DATA_LOW -> fetchTileDataLow();
            case FETCH_TILE_DATA_HIGH -> fetchTileDataHigh();
            case PUSH_TO_FIFO -> pushToFifo();
        };
    }

    private Step fetchTileNo() {
        int tileIndex = getTilemapIndex(X_POSITION_COUNTER, registers.read(LY), 0, 0);
        short tileNumberAddress = (short) (BACKGROUND_MAP_A_ADDRESS + tileIndex);

        currentTileNumber = memory.read(tileNumberAddress);

        clock.tick();
        clock.tick();
        return Step.FETCH_TILE_DATA_LOW;
    }

    private Step fetchTileDataLow() {
        int tileDataAddress = getSignedTileNumberAddress(currentTileNumber);
        int rowDataAddress = getTileRow(tileDataAddress, registers.read(LY), 0);
        tileDataLow = memory.read((short) rowDataAddress);
        clock.tick();
        clock.tick();
        return Step.FETCH_TILE_DATA_HIGH;
    }

    private Step fetchTileDataHigh() {
        int tileDataAddress = getSignedTileNumberAddress(currentTileNumber);
        int rowDataAddress = 1 + getTileRow(tileDataAddress, registers.read(LY), 0);
        tileDataHigh = memory.read((short) rowDataAddress);
        clock.tick();
        clock.tick();
        return Step.PUSH_TO_FIFO;
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
        if (uint(registers.read(LY)) == 0) history.getLast().addAll(pixelData);
        backgroundFifo.write(pixelData);
        clock.tick();
        X_POSITION_COUNTER++;
        return Step.FETCH_TILE_NO;
    }

    private enum Step {
        FETCH_TILE_NO,
        FETCH_TILE_DATA_LOW,
        FETCH_TILE_DATA_HIGH,
        PUSH_TO_FIFO;

        public Step next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
    }

    public static String printList(List<?> list) {
        String result = list.stream()
                .limit(160)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        return result;
    }
}
