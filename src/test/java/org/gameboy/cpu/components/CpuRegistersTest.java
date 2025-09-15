package org.gameboy.cpu.components;

import org.gameboy.cpu.Flag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;

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
        
        assertThatHex(registers.A()).isEqualTo(value);
        assertThatHex((byte)(registers.AF() >> 8)).isEqualTo(value);
        assertThatHex((byte)registers.AF()).isEqualTo((byte)0); // F register (flags) should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterB(byte value) {
        registers.setB(value);
        
        assertThatHex(registers.B()).isEqualTo(value);
        assertThatHex((byte)(registers.BC() >> 8)).isEqualTo(value);
        assertThatHex(registers.C()).isEqualTo((byte)0); // C register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterC(byte value) {
        registers.setC(value);
        
        assertThatHex(registers.C()).isEqualTo(value);
        assertThatHex((byte)registers.BC()).isEqualTo(value);
        assertThatHex(registers.B()).isEqualTo((byte)0); // B register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterD(byte value) {
        registers.setD(value);
        
        assertThatHex(registers.D()).isEqualTo(value);
        assertThatHex((byte)(registers.DE() >> 8)).isEqualTo(value);
        assertThatHex(registers.E()).isEqualTo((byte)0); // E register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterE(byte value) {
        registers.setE(value);
        
        assertThatHex(registers.E()).isEqualTo(value);
        assertThatHex((byte)registers.DE()).isEqualTo(value);
        assertThatHex(registers.D()).isEqualTo((byte)0); // D register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterH(byte value) {
        registers.setH(value);
        
        assertThatHex(registers.H()).isEqualTo(value);
        assertThatHex((byte)(registers.HL() >> 8)).isEqualTo(value);
        assertThatHex(registers.L()).isEqualTo((byte)0); // L register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(bytes = {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF, 0x42, (byte) 0xAB})
    void testRegisterL(byte value) {
        registers.setL(value);
        
        assertThatHex(registers.L()).isEqualTo(value);
        assertThatHex((byte)registers.HL()).isEqualTo(value);
        assertThatHex(registers.H()).isEqualTo((byte)0); // H register should remain 0
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterBC(short value) {
        registers.setBC(value);
        
        assertThatHex(registers.BC()).isEqualTo(value);
        assertThatHex(registers.B()).isEqualTo((byte)(value >> 8));
        assertThatHex(registers.C()).isEqualTo((byte)value);
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterDE(short value) {
        registers.setDE(value);
        
        assertThatHex(registers.DE()).isEqualTo(value);
        assertThatHex(registers.D()).isEqualTo((byte)(value >> 8));
        assertThatHex(registers.E()).isEqualTo((byte)value);
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterHL(short value) {
        registers.setHL(value);
        
        assertThatHex(registers.HL()).isEqualTo(value);
        assertThatHex(registers.H()).isEqualTo((byte)(value >> 8));
        assertThatHex(registers.L()).isEqualTo((byte)value);
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0010, 0x7FF0, (short) 0x8000, (short) 0xFFF0, 0x1230, (short) 0xABC0})
    void testRegisterAF(short value) {
        registers.setAF(value);
        
        assertThatHex(registers.AF()).isEqualTo(value);
        assertThatHex(registers.A()).isEqualTo((byte)(value >> 8));
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterSP(short value) {
        registers.setSP(value);
        
        assertThatHex(registers.SP()).isEqualTo(value);
    }
    
    @ParameterizedTest
    @ValueSource(shorts = {0x0000, 0x0001, 0x7FFF, (short) 0x8000, (short) 0xFFFF, 0x1234, (short) 0xABCD})
    void testRegisterPC(short value) {
        registers.setPC(value);
        
        assertThatHex(registers.PC()).isEqualTo(value);
    }
    
    @Test
    void testZeroFlag() {
        assertThat(registers.getFlag(Flag.Z)).isFalse();
        
        registers.setFlag(Flag.Z, true);
        assertThat(registers.getFlag(Flag.Z)).isTrue();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0080);
        
        registers.setFlag(Flag.Z, false);
        assertThat(registers.getFlag(Flag.Z)).isFalse();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0000);
    }
    
    @Test
    void testNFlag() {
        assertThat(registers.getFlag(Flag.N)).isFalse();
        
        registers.setFlag(Flag.N, true);
        assertThat(registers.getFlag(Flag.N)).isTrue();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0040);
        
        registers.setFlag(Flag.N, false);
        assertThat(registers.getFlag(Flag.N)).isFalse();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0000);
    }
    
    @Test
    void testHFlag() {
        assertThat(registers.getFlag(Flag.H)).isFalse();
        
        registers.setFlag(Flag.H, true);
        assertThat(registers.getFlag(Flag.H)).isTrue();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0020);
        
        registers.setFlag(Flag.H, false);
        assertThat(registers.getFlag(Flag.H)).isFalse();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0000);
    }
    
    @Test
    void testCFlag() {
        assertThat(registers.getFlag(Flag.C)).isFalse();
        
        registers.setFlag(Flag.C, true);
        assertThat(registers.getFlag(Flag.C)).isTrue();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0010);
        
        registers.setFlag(Flag.C, false);
        assertThat(registers.getFlag(Flag.C)).isFalse();
        assertThatHex(registers.AF()).isEqualTo((short) 0x0000);
    }
    
    @Test
    void testFRegisterLowerNibbleMasking() {
        registers.setAF((short) 0x34df);  // A=0x34, F=0xD0 (Z=1, N=1, H=0, C=1, lower nibble=f)
        assertThatHex(registers.AF()).isEqualTo((short) 0x34d0);
        assertThat(registers.getFlag(Flag.Z)).isTrue();
        assertThat(registers.getFlag(Flag.N)).isTrue();
        assertThat(registers.getFlag(Flag.H)).isFalse();
        assertThat(registers.getFlag(Flag.C)).isTrue();
    }
    
    @Test
    void testConstructorFRegisterMasking() {
        // Test that constructor also masks the lower nibble of F register
        CpuRegisters regs = new CpuRegisters(
            (short) 0x12FF,  // AF with lower nibble set
            (short) 0x3456,  // BC
            (short) 0x789A,  // DE
            (short) 0xBCDE,  // HL
            (short) 0xFFFE,  // SP
            (short) 0x0100,  // PC
            (byte) 0x00,     // instructionRegister
            false            // IME
        );
        
        assertThatHex(regs.AF()).isEqualTo((short) 0x12F0);
    }
    
    @Test
    void testMultipleFlags() {
        registers.setFlag(Flag.Z, true);
        registers.setFlag(Flag.C, true);
        
        assertThat(registers.getFlag(Flag.Z)).isTrue();
        assertThat(registers.getFlag(Flag.C)).isTrue();
        assertThat(registers.getFlag(Flag.N)).isFalse();
        assertThat(registers.getFlag(Flag.H)).isFalse();
        
        assertThatHex(registers.AF()).isEqualTo((short) 0x0090);
    }
    
    @Test
    void testSetFlagsWithVarargs() {
        registers.setFlags(true, Flag.Z, Flag.H);
        
        assertThat(registers.getFlag(Flag.Z)).isTrue();
        assertThat(registers.getFlag(Flag.H)).isTrue();
        assertThat(registers.getFlag(Flag.N)).isFalse();
        assertThat(registers.getFlag(Flag.C)).isFalse();
        
        registers.setFlags(false, Flag.Z, Flag.H);
        
        assertThat(registers.getFlag(Flag.Z)).isFalse();
        assertThat(registers.getFlag(Flag.H)).isFalse();
    }
    
    @Test
    void testSetFlagsWithHashtable() {
        Hashtable<Flag, Boolean> changeset = new Hashtable<>();
        changeset.put(Flag.Z, true);
        changeset.put(Flag.N, false);
        changeset.put(Flag.H, true);
        changeset.put(Flag.C, false);
        
        registers.setFlags(changeset);
        
        assertThat(registers.getFlag(Flag.Z)).isTrue();
        assertThat(registers.getFlag(Flag.N)).isFalse();
        assertThat(registers.getFlag(Flag.H)).isTrue();
        assertThat(registers.getFlag(Flag.C)).isFalse();
        
        assertThatHex(registers.AF()).isEqualTo((short) 0x00A0);
    }
    
    @Test
    void testFlagsDoNotAffectARegister() {
        registers.setA((byte) 0x42);
        registers.setFlag(Flag.Z, true);
        registers.setFlag(Flag.N, true);
        registers.setFlag(Flag.H, true);
        registers.setFlag(Flag.C, true);
        
        assertThatHex(registers.A()).isEqualTo((byte) 0x42);
        assertThatHex(registers.AF()).isEqualTo((short) 0x42F0);
    }
    
    @Test
    void testIME() {
        assertThat(registers.IME()).isFalse();
        
        registers.setIME(true);
        assertThat(registers.IME()).isTrue();
        
        registers.setIME(false);
        assertThat(registers.IME()).isFalse();
    }
    
    @Test
    void testInstructionRegister() {
        assertThatHex(registers.instructionRegister()).isEqualTo((byte) 0x00);
        
        registers.setInstructionRegister((byte) 0x42);
        assertThatHex(registers.instructionRegister()).isEqualTo((byte) 0x42);
        
        registers.setInstructionRegister((byte) 0xFF);
        assertThatHex(registers.instructionRegister()).isEqualTo((byte) 0xFF);
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
        
        assertThatHex(customRegisters.AF()).isEqualTo((short) 0x1230);  // af lower nibble should always be 0
        assertThatHex(customRegisters.BC()).isEqualTo((short) 0x5678);
        assertThatHex(customRegisters.DE()).isEqualTo((short) 0x9ABC);
        assertThatHex(customRegisters.HL()).isEqualTo((short) 0xDEF0);
        assertThatHex(customRegisters.SP()).isEqualTo((short) 0xFFFE);
        assertThatHex(customRegisters.PC()).isEqualTo((short) 0x0100);
        assertThatHex(customRegisters.instructionRegister()).isEqualTo((byte) 0xCB);
        assertThat(customRegisters.IME()).isTrue();
    }
}