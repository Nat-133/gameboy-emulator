package org.gameboy.common;

@FunctionalInterface
public interface FramePacer {
    void sync();
}
