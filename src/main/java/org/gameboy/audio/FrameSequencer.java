package org.gameboy.audio;

public class FrameSequencer {
    private static final int PERIOD = 8192;

    private final Runnable lengthCallback;
    private final Runnable sweepCallback;
    private final Runnable envelopeCallback;

    private int timer;
    private int step;

    public FrameSequencer(Runnable lengthCallback, Runnable sweepCallback, Runnable envelopeCallback) {
        this.lengthCallback = lengthCallback;
        this.sweepCallback = sweepCallback;
        this.envelopeCallback = envelopeCallback;
        this.timer = PERIOD;
        this.step = 0;
    }

    public void tCycle() {
        timer--;
        if (timer <= 0) {
            timer = PERIOD;
            clockStep();
            step = (step + 1) & 7;
        }
    }

    private void clockStep() {
        switch (step) {
            case 0, 4 -> lengthCallback.run();
            case 2 -> { lengthCallback.run(); sweepCallback.run(); }
            case 6 -> { lengthCallback.run(); sweepCallback.run(); }
            case 7 -> envelopeCallback.run();
            default -> {}
        }
    }

    public void reset() {
        timer = PERIOD;
        step = 0;
    }

    public int getStep() {
        return step;
    }
}
