package org.gameboy.common;

public interface Cartridge {
    byte read(short address);
    void write(short address, byte value);
}
