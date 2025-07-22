package org.gameboy.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappedMemoryTest {

    private ByteRegister divRegister;
    private ByteRegister timaRegister;
    private ByteRegister tmaRegister;
    private ByteRegister tacRegister;
    private MappedMemory memory;

    @BeforeEach
    public void setUp() {
        divRegister = new IntBackedRegister();
        timaRegister = new IntBackedRegister();
        tmaRegister = new IntBackedRegister();
        tacRegister = new IntBackedRegister();
        memory = new MappedMemory(divRegister, timaRegister, tmaRegister, tacRegister);
    }

    @Test
    public void givenDivRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x42;
        divRegister.write(expectedValue);
        
        assertEquals(expectedValue, memory.read((short) 0xFF04));
    }

    @Test
    public void givenMemoryIsWrittenToDivAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x24;
        memory.write((short) 0xFF04, expectedValue);
        
        assertEquals(expectedValue, divRegister.read());
    }

    @Test
    public void givenTimaRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x55;
        timaRegister.write(expectedValue);
        
        assertEquals(expectedValue, memory.read((short) 0xFF05));
    }

    @Test
    public void givenMemoryIsWrittenToTimaAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0xAA;
        memory.write((short) 0xFF05, expectedValue);
        
        assertEquals(expectedValue, timaRegister.read());
    }

    @Test
    public void givenTmaRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x33;
        tmaRegister.write(expectedValue);
        
        assertEquals(expectedValue, memory.read((short) 0xFF06));
    }

    @Test
    public void givenMemoryIsWrittenToTmaAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0xCC;
        memory.write((short) 0xFF06, expectedValue);
        
        assertEquals(expectedValue, tmaRegister.read());
    }

    @Test
    public void givenTacRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x77;
        tacRegister.write(expectedValue);
        
        assertEquals(expectedValue, memory.read((short) 0xFF07));
    }

    @Test
    public void givenMemoryIsWrittenToTacAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x88;
        memory.write((short) 0xFF07, expectedValue);
        
        assertEquals(expectedValue, tacRegister.read());
    }
    
    @Test
    public void testUnmappedAddressesUseDefaultMemory() {
        short unmappedAddress = (short) 0x1000;
        
        assertEquals((byte) 0x00, memory.read(unmappedAddress));
        
        memory.write(unmappedAddress, (byte) 0x99);
        assertEquals((byte) 0x99, memory.read(unmappedAddress));
    }
}