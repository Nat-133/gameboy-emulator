package org.gameboy.components;

public class CpuClock {
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
}
