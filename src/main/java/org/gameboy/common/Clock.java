package org.gameboy.common;

public interface Clock {
    void tick();

    long getTime();

    void stop();

    void start();
}
