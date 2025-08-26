package org.gameboy.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class SerialControllerTest {
    
    private SerialController serialController;
    private InterruptController mockInterruptController;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    
    @BeforeEach
    public void setUp() {
        mockInterruptController = mock(InterruptController.class);
        serialController = new SerialController(mockInterruptController);
        
        originalOut = System.out;
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }
    
    @Test
    public void testSerialDataReadWrite() {
        byte testValue = (byte) 0x42;
        
        serialController.writeSerialData(testValue);
        assertThatHex(serialController.readSerialData()).isEqualTo(testValue);
    }
    
    @Test
    public void testSerialControlReadWrite() {
        byte testValue = (byte) 0x7F;
        
        serialController.writeSerialControl(testValue);
        assertThatHex(serialController.readSerialControl()).isEqualTo(testValue);
    }
    
    @Test
    public void testSerialTransferWithInterrupt() {
        byte testChar = (byte) 'A';
        serialController.writeSerialData(testChar);
        
        serialController.writeSerialControl((byte) 0x81);
        
        assertEquals("A", outputStream.toString());
        
        assertEquals("A", serialController.getOutput());
        
        verify(mockInterruptController).setInterrupt(Interrupt.SERIAL);
        
        assertThatHex(serialController.readSerialData()).isEqualTo((byte) 0xFF);
        
        assertThatHex(serialController.readSerialControl()).isEqualTo((byte) 0x01);
    }
    
    @Test
    public void testNoTransferWithoutProperControl() {
        byte testChar = (byte) 'B';
        serialController.writeSerialData(testChar);
        
        serialController.writeSerialControl((byte) 0x01);
        
        assertEquals("", outputStream.toString());
        
        verify(mockInterruptController, never()).setInterrupt(any());
        
        assertThatHex(serialController.readSerialData()).isEqualTo(testChar);
    }
    
    @Test
    public void testMultipleCharacterOutput() {
        String expectedOutput = "Hello";
        
        for (char c : expectedOutput.toCharArray()) {
            serialController.writeSerialData((byte) c);
            serialController.writeSerialControl((byte) 0x81);
        }
        
        assertEquals(expectedOutput, outputStream.toString());
        assertEquals(expectedOutput, serialController.getOutput());
        
        verify(mockInterruptController, times(5)).setInterrupt(Interrupt.SERIAL);
    }
    
    @Test
    public void testClearOutput() {
        serialController.writeSerialData((byte) 'X');
        serialController.writeSerialControl((byte) 0x81);
        
        assertEquals("X", serialController.getOutput());
        
        serialController.clearOutput();
        
        assertEquals("", serialController.getOutput());
    }
    
    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }
}