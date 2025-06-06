package org.gameboy.common;

public class ClockWithParallelProcess implements Clock{
    private final Runnable parallelProcess;
    private int time;

    public ClockWithParallelProcess(Runnable parallelProcess) {
        this.parallelProcess = parallelProcess;
        time = 0;
    }

    @Override
    public void tick() {
        parallelProcess.run();
        time ++;
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
