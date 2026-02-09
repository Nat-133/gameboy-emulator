package org.gameboy.common;

import org.gameboy.components.TacRegister;
import org.gameboy.components.joypad.JoypadController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("FieldCanBeLocal")
public class MappedMemoryTest {

    private ByteRegister divRegister;
    private ByteRegister timaRegister;
    private ByteRegister tmaRegister;
    private TacRegister tacRegister;
    private ByteRegister dmaRegister;
    private ByteRegister interruptFlagsRegister;
    private ByteRegister interruptEnableRegister;
    private ByteRegister lcdcRegister;
    private ByteRegister statRegister;
    private ByteRegister scyRegister;
    private ByteRegister scxRegister;
    private ByteRegister lyRegister;
    private ByteRegister lycRegister;
    private ByteRegister wyRegister;
    private ByteRegister wxRegister;
    private ByteRegister bgpRegister;
    private ByteRegister obp0Register;
    private ByteRegister obp1Register;
    private SerialController serialController;
    private JoypadController joypadController;
    private MappedMemory memory;

    @BeforeEach
    public void setUp() {
        divRegister = new IntBackedRegister();
        timaRegister = new IntBackedRegister();
        tmaRegister = new IntBackedRegister();
        tacRegister = new TacRegister();
        dmaRegister = new IntBackedRegister();
        interruptFlagsRegister = new IntBackedRegister();
        interruptEnableRegister = new IntBackedRegister();
        lcdcRegister = new IntBackedRegister();
        statRegister = new IntBackedRegister();
        scyRegister = new IntBackedRegister();
        scxRegister = new IntBackedRegister();
        lyRegister = new IntBackedRegister();
        lycRegister = new IntBackedRegister();
        wyRegister = new IntBackedRegister();
        wxRegister = new IntBackedRegister();
        bgpRegister = new IntBackedRegister();
        obp0Register = new IntBackedRegister();
        obp1Register = new IntBackedRegister();
        serialController = Mockito.mock(SerialController.class);
        joypadController = Mockito.mock(JoypadController.class);
        memory = new MappedMemory(divRegister, timaRegister, tmaRegister, tacRegister, dmaRegister,
                                 interruptFlagsRegister, interruptEnableRegister,
                                 lcdcRegister, statRegister, scyRegister, scxRegister,
                                 lyRegister, lycRegister, wyRegister, wxRegister,
                                 bgpRegister, obp0Register, obp1Register,
                                 serialController, joypadController);
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
        byte writeValue = (byte) 0x07;
        tacRegister.write(writeValue);

        assertEquals((byte) 0xFF, memory.read((short) 0xFF07));
    }

    @Test
    public void givenMemoryIsWrittenToTacAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte writeValue = (byte) 0x05;
        memory.write((short) 0xFF07, writeValue);

        assertEquals((byte) 0xFD, tacRegister.read());
    }
    
    @Test
    public void testUnmappedAddressesUseDefaultMemory() {
        short unmappedAddress = (short) 0x1000;
        
        assertEquals((byte) 0x00, memory.read(unmappedAddress));
        
        memory.write(unmappedAddress, (byte) 0x99);
        assertEquals((byte) 0x99, memory.read(unmappedAddress));
    }
    
    @Test
    public void givenLcdcRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x91;
        lcdcRegister.write(expectedValue);
        
        assertEquals(expectedValue, memory.read((short) 0xFF40));
    }
    
    @Test
    public void givenMemoryIsWrittenToWyAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x45;
        memory.write((short) 0xFF4A, expectedValue);
        
        assertEquals(expectedValue, wyRegister.read());
    }
    
    @Test
    public void givenMemoryIsWrittenToWxAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x67;
        memory.write((short) 0xFF4B, expectedValue);
        
        assertEquals(expectedValue, wxRegister.read());
    }
    
    @Test
    public void testSerialDataMemoryMapping() {
        byte testValue = (byte) 0xAB;
        Mockito.when(serialController.readSerialData()).thenReturn(testValue);
        
        assertEquals(testValue, memory.read((short) 0xFF01));
        
        byte writeValue = (byte) 0xCD;
        memory.write((short) 0xFF01, writeValue);
        Mockito.verify(serialController).writeSerialData(writeValue);
    }
    
    @Test
    public void testSerialControlMemoryMapping() {
        byte testValue = (byte) 0x81;
        Mockito.when(serialController.readSerialControl()).thenReturn(testValue);

        assertEquals(testValue, memory.read((short) 0xFF02));

        byte writeValue = (byte) 0x01;
        memory.write((short) 0xFF02, writeValue);
        Mockito.verify(serialController).writeSerialControl(writeValue);
    }

    @Test
    public void testJoypadMemoryMapping() {
        byte testValue = (byte) 0xCF;
        Mockito.when(joypadController.read()).thenReturn(testValue);

        assertEquals(testValue, memory.read((short) 0xFF00));

        byte writeValue = (byte) 0x20;
        memory.write((short) 0xFF00, writeValue);
        Mockito.verify(joypadController).write(writeValue);
    }

    @Test
    public void givenScxRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x42;
        scxRegister.write(expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF43));
    }

    @Test
    public void givenMemoryIsWrittenToScxAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0xAB;
        memory.write((short) 0xFF43, expectedValue);

        assertEquals(expectedValue, scxRegister.read());
    }

    @Test
    public void givenScyRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x89;
        scyRegister.write(expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF42));
    }

    @Test
    public void givenMemoryIsWrittenToScyAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0xCD;
        memory.write((short) 0xFF42, expectedValue);

        assertEquals(expectedValue, scyRegister.read());
    }

    @Test
    public void givenBgpRegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0xE4;  // Common BGP value
        bgpRegister.write(expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF47));
    }

    @Test
    public void givenMemoryIsWrittenToBgpAddress_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x1B;
        memory.write((short) 0xFF47, expectedValue);

        assertEquals(expectedValue, bgpRegister.read());
    }

    @Test
    public void givenObp0RegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0xD2;
        obp0Register.write(expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF48));
    }

    @Test
    public void givenMemoryIsWrittenToObp0Address_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x3F;
        memory.write((short) 0xFF48, expectedValue);

        assertEquals(expectedValue, obp0Register.read());
    }

    @Test
    public void givenObp1RegisterIsWritten_whenMemoryIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x90;
        obp1Register.write(expectedValue);

        assertEquals(expectedValue, memory.read((short) 0xFF49));
    }

    @Test
    public void givenMemoryIsWrittenToObp1Address_whenRegisterIsRead_thenCorrectValueIsReturned() {
        byte expectedValue = (byte) 0x6C;
        memory.write((short) 0xFF49, expectedValue);

        assertEquals(expectedValue, obp1Register.read());
    }

    @Test
    public void testPaletteRegistersDefaultValues() {
        memory.write((short) 0xFF47, (byte) 0xFC);
        assertEquals((byte) 0xFC, memory.read((short) 0xFF47));

        memory.write((short) 0xFF48, (byte) 0xFF);
        assertEquals((byte) 0xFF, memory.read((short) 0xFF48));

        memory.write((short) 0xFF49, (byte) 0xFF);
        assertEquals((byte) 0xFF, memory.read((short) 0xFF49));
    }
}