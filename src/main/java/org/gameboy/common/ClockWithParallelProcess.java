package org.gameboy.common;

public class ClockWithParallelProcess implements Clock{
    private static final int TICKS_PER_FRAME = 17_556; // 70,224 T-cycles / 4 T-cycles per tick

    private final Runnable parallelProcess;
    private final FramePacer framePacer;
    private int time;
    private int ticksSinceSync;

    public ClockWithParallelProcess(Runnable parallelProcess) {
        this(parallelProcess, () -> {});
    }

    public ClockWithParallelProcess(Runnable parallelProcess, FramePacer framePacer) {
        this.parallelProcess = parallelProcess;
        this.framePacer = framePacer;
        time = 0;
        ticksSinceSync = 0;
    }

    @Override
    public void tick() {
        parallelProcess.run();
        time ++;
        ticksSinceSync++;
        if (ticksSinceSync >= TICKS_PER_FRAME) {
            ticksSinceSync = 0;
            framePacer.sync();
        }
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void stop() {

    }

    @Override
    public void start() {

    }
}
