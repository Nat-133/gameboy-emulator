package org.gameboy.common;

public record MemoryDump(short startAddress, byte[] memory) {
    
    public static MemoryDump fromZero(byte[] memory) {
        return new MemoryDump((short) 0, memory);
    }
    
    public int length() {
        return memory.length;
    }
    
    public short endAddress() {
        return (short) (startAddress + memory.length);
    }
}