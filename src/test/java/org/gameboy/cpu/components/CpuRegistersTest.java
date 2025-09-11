package org.gameboy.cpu.components;

import org.gameboy.cpu.Flag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class CpuRegistersTest {
    
    private CpuRegisters registers;
    
    @BeforeEach
    void setUp() {
        registers = new CpuRegisters(
            (short) 0x0000,  // AF
            (short) 0x0000,  // BC
            (short) 0x0000,  // DE
            (short) 0x0000,  // HL
            (short) 0x0000,  // SP
            (short) 0x0000,  // PC
            (byte) 0x00,     // instruction register
            false            // IME
        );
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterA(byte value) {
        registers.setA(value);
        
        assertEquals(value, registers.A());
        assertEquals(value, (byte)(registers.AF() >> 8));
        assertEquals((byte)0, (byte)registers.AF()); // F register (flags) should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterB(byte value) {
        registers.setB(value);
        
        assertEquals(value, registers.B());
        assertEquals(value, (byte)(registers.BC() >> 8));
        assertEquals((byte)0, registers.C()); // C register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterC(byte value) {
        registers.setC(value);
        
        assertEquals(value, registers.C());
        assertEquals(value, (byte)registers.BC());
        assertEquals((byte)0, registers.B()); // B register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterD(byte value) {
        registers.setD(value);
        
        assertEquals(value, registers.D());
        assertEquals(value, (byte)(registers.DE() >> 8));
        assertEquals((byte)0, registers.E()); // E register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterE(byte value) {
        registers.setE(value);
        
        assertEquals(value, registers.E());
        assertEquals(value, (byte)registers.DE());
        assertEquals((byte)0, registers.D()); // D register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterH(byte value) {
        registers.setH(value);
        
        assertEquals(value, registers.H());
        assertEquals(value, (byte)(registers.HL() >> 8));
        assertEquals((byte)0, registers.L()); // L register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterL(byte value) {
        registers.setL(value);
        
        assertEquals(value, registers.L());
        assertEquals(value, (byte)registers.HL());
        assertEquals((byte)0, registers.H()); // H register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterBC(short value) {
        registers.setBC(value);
        
        assertEquals(value, registers.BC());
        assertEquals((byte)(value >> 8), registers.B());
        assertEquals((byte)value, registers.C());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterDE(short value) {
        registers.setDE(value);
        
        assertEquals(value, registers.DE());
        assertEquals((byte)(value >> 8), registers.D());
        assertEquals((byte)value, registers.E());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterHL(short value) {
        registers.setHL(value);
        
        assertEquals(value, registers.HL());
        assertEquals((byte)(value >> 8), registers.H());
        assertEquals((byte)value, registers.L());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0010, 0x7FF0, (short) 0x8000, (short) 0xFFF0, 0x1230, (short) 0xABC0})
    void testRegisterAF(short value) {
        registers.setAF(value);
        
        assertEquals(value, registers.AF());
        assertEquals((byte)(value >> 8), registers.A());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterSP(short value) {
        registers.setSP(value);
        
        assertEquals(value, registers.SP());
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterPC(short value) {
        registers.setPC(value);
        
        assertEquals(value, registers.PC());
    }
    
    @Test
    void testZeroFlag() {
        assertFalse(registers.getFlag(Flag.Z));
        
        registers.setFlag(Flag.Z, true);
        assertTrue(registers.getFlag(Flag.Z));
        assertEquals((short) 0x0080, registers.AF());
        
        registers.setFlag(Flag.Z, false);
        assertFalse(registers.getFlag(Flag.Z));
        assertEquals((short) 0x0000, registers.AF());
    }
    
    @Test
    void testNFlag() {
        assertFalse(registers.getFlag(Flag.N));
        
        registers.setFlag(Flag.N, true);
        assertTrue(registers.getFlag(Flag.N));
        assertEquals((short) 0x0040, registers.AF());
        
        registers.setFlag(Flag.N, false);
        assertFalse(registers.getFlag(Flag.N));
        assertEquals((short) 0x0000, registers.AF());
    }
    
    @Test
    void testHFlag() {
        assertFalse(registers.getFlag(Flag.H));
        
        registers.setFlag(Flag.H, true);
        assertTrue(registers.getFlag(Flag.H));
        assertEquals((short) 0x0020, registers.AF());
        
        registers.setFlag(Flag.H, false);
        assertFalse(registers.getFlag(Flag.H));
        assertEquals((short) 0x0000, registers.AF());
    }
    
    @Test
    void testCFlag() {
        assertFalse(registers.getFlag(Flag.C));
        
        registers.setFlag(Flag.C, true);
        assertTrue(registers.getFlag(Flag.C));
        assertEquals((short) 0x0010, registers.AF());
        
        registers.setFlag(Flag.C, false);
        assertFalse(registers.getFlag(Flag.C));
        assertEquals((short) 0x0000, registers.AF());
    }
    
    @Test
    void testMultipleFlags() {
        registers.setFlag(Flag.Z, true);
        registers.setFlag(Flag.C, true);
        
        assertTrue(registers.getFlag(Flag.Z));
        assertTrue(registers.getFlag(Flag.C));
        assertFalse(registers.getFlag(Flag.N));
        assertFalse(registers.getFlag(Flag.H));
        
        assertEquals((short) 0x0090, registers.AF());
    }
    
    @Test
    void testSetFlagsWithVarargs() {
        registers.setFlags(true, Flag.Z, Flag.H);
        
        assertTrue(registers.getFlag(Flag.Z));
        assertTrue(registers.getFlag(Flag.H));
        assertFalse(registers.getFlag(Flag.N));
        assertFalse(registers.getFlag(Flag.C));
        
        registers.setFlags(false, Flag.Z, Flag.H);
        
        assertFalse(registers.getFlag(Flag.Z));
        assertFalse(registers.getFlag(Flag.H));
    }
    
    @Test
    void testSetFlagsWithHashtable() {
        Hashtable<Flag, Boolean> changeset = new Hashtable<>();
        changeset.put(Flag.Z, true);
        changeset.put(Flag.N, false);
        changeset.put(Flag.H, true);
        changeset.put(Flag.C, false);
        
        registers.setFlags(changeset);
        
        assertTrue(registers.getFlag(Flag.Z));
        assertFalse(registers.getFlag(Flag.N));
        assertTrue(registers.getFlag(Flag.H));
        assertFalse(registers.getFlag(Flag.C));
        
        assertEquals((short) 0x00A0, registers.AF());
    }
    
    @Test
    void testFlagsDoNotAffectARegister() {
        registers.setA((byte) 0x42);
        registers.setFlag(Flag.Z, true);
        registers.setFlag(Flag.N, true);
        registers.setFlag(Flag.H, true);
        registers.setFlag(Flag.C, true);
        
        assertEquals((byte) 0x42, registers.A());
        assertEquals((short) 0x42F0, registers.AF());
    }
    
    @Test
    void testIME() {
        assertFalse(registers.IME());
        
        registers.setIME(true);
        assertTrue(registers.IME());
        
        registers.setIME(false);
        assertFalse(registers.IME());
    }
    
    @Test
    void testInstructionRegister() {
        assertEquals((byte) 0x00, registers.instructionRegister());
        
        registers.setInstructionRegister((byte) 0x42);
        assertEquals((byte) 0x42, registers.instructionRegister());
        
        registers.setInstructionRegister((byte) 0xFF);
        assertEquals((byte) 0xFF, registers.instructionRegister());
    }
    
    @Test
    void testConstructorInitializesAllValues() {
        CpuRegisters customRegisters = new CpuRegisters(
            (short) 0x1234,  // AF
            (short) 0x5678,  // BC
            (short) 0x9ABC,  // DE
            (short) 0xDEF0,  // HL
            (short) 0xFFFE,  // SP
            (short) 0x0100,  // PC
            (byte) 0xCB,     // instruction register
            true             // IME
        );
        
        assertEquals((short) 0x1234, customRegisters.AF());
        assertEquals((short) 0x5678, customRegisters.BC());
        assertEquals((short) 0x9ABC, customRegisters.DE());
        assertEquals((short) 0xDEF0, customRegisters.HL());
        assertEquals((short) 0xFFFE, customRegisters.SP());
        assertEquals((short) 0x0100, customRegisters.PC());
        assertEquals((byte) 0xCB, customRegisters.instructionRegister());
        assertTrue(customRegisters.IME());
    }
}