package org.gameboy.components;

public interface Clock {
    void tick();

    long getTime();

    void stop();

    void start();
}
