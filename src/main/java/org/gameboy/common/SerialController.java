package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SerialController {
    private byte serialData = 0;
    private byte serialControl = 0;
    private final InterruptController interruptController;
    private final StringBuilder outputBuffer = new StringBuilder();
    
    @Inject
    public SerialController(InterruptController interruptController) {
        this.interruptController = interruptController;
    }
    
    public byte readSerialData() {
        return serialData;
    }
    
    public void writeSerialData(byte value) {
        serialData = value;
    }
    
    public byte readSerialControl() {
        return serialControl;
    }
    
    public void writeSerialControl(byte value) {
        serialControl = value;
        
        if ((value & 0x81) == 0x81) {
            int unsignedByte = serialData & 0xFF;
            
            if ((unsignedByte >= 32 && unsignedByte <= 126) ||
                unsignedByte == '\n' || unsignedByte == '\r' || unsignedByte == '\t') {
                char character = (char) unsignedByte;
                System.out.print(character);
                outputBuffer.append(character);
            } else {
                String hexString = String.format("[0x%02X]", unsignedByte);
                System.out.print(hexString);
                outputBuffer.append(hexString);
            }
            
            serialData = (byte) 0xFF;
            serialControl = (byte) (serialControl & 0x7F);
            
            interruptController.setInterrupt(Interrupt.SERIAL);
        }
    }
    
    public String getOutput() {
        return outputBuffer.toString();
    }
    
    public void clearOutput() {
        outputBuffer.setLength(0);
    }
}