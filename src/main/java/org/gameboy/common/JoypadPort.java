package org.gameboy.common;

public interface JoypadPort {
    byte read();
    void write(byte value);
}
