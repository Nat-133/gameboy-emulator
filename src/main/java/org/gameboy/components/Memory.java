package org.gameboy.components;

public interface Memory {
    byte read(short address);

    void write(short address, byte value);

    void registerMemoryListener(short address, MemoryListener listener);
}
