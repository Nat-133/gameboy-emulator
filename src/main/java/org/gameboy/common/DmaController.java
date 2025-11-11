package org.gameboy.common;

public interface DmaController {
    void startDma(byte sourceHigh);

    void mCycle();

    boolean isDmaActive();
}
