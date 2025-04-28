package org.gameboy.cpu.components;

import org.gameboy.common.Clock;

public class CpuClock implements Clock {
    private long time;

    public CpuClock() {
        this.time = 0;
    }

    public void tick() {
        this.time++;
    }

    public long getTime() {
        return this.time;
    }

    public void stop() {}

    public void start() {}
}
