package org.gameboy.common;

public interface ByteRegister {
    byte read();

    void write(byte b);
}
