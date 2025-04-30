package org.gameboy.common;

import java.util.concurrent.Phaser;
import java.util.concurrent.locks.LockSupport;

public class SynchronisedClock implements Clock{
    private long lastTickTime;
    private Phaser phaser;

    public SynchronisedClock() {
        this.phaser = new Phaser(1);
    }

    public void registerParallelOperation() {
        phaser.register();
    }

    public void unregisterParallelOperation() {
        phaser.arriveAndDeregister();
    }

    @Override
    public void tick() {
        phaser.arriveAndAwaitAdvance();
        long currentTime = System.nanoTime();
        long cycleTime = 1_000_000_000L/4_096_254L;

        if (currentTime < lastTickTime + cycleTime) {
            LockSupport.parkNanos(cycleTime - (currentTime - lastTickTime));
        }
        lastTickTime = lastTickTime + cycleTime;
    }

    @Override
    public long getTime() {
        return 0;
    }

    @Override
    public void stop() {

    }

    @Override
    public void start() {

    }
}
