package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.targets.Target;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Hashtable;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.GameboyAssertions.assertThatHex;

class DecimalAdjustAcumulatorTest {

    static Stream<Arguments> getBytes() {
        return Stream.of(
                Arguments.of(Named.of("0x75", 0x75), Named.of("0x25", 0x25)),
                Arguments.of(Named.of("0x00", 0x00), Named.of("0x00", 0x00)),
                Arguments.of(Named.of("0x01", 0x01), Named.of("0x01", 0x01)),
                Arguments.of(Named.of("0x05", 0x05), Named.of("0x05", 0x05)),
                Arguments.of(Named.of("0x90", 0x90), Named.of("0x10", 0x10)),
                Arguments.of(Named.of("0x99", 0x99), Named.of("0x94", 0x94)),
                Arguments.of(Named.of("0x55", 0x55), Named.of("0x65", 0x65))
        );
    }

    @ParameterizedTest
    @MethodSource("getBytes")
    void givenTwoBytes_whenAddThenDecimalAdjust_thenResultIsDecimal(int a, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .withB(b)
                .build();

        Add.add_a_r8(Target.b).execute(cpuStructure);
        DecimalAdjustAcumulator.daa().execute(cpuStructure);

        int expected_bcd = decimalAdd(a, b);
        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.C, expected_bcd >= 0x100)
                .with(Flag.Z, (expected_bcd & 0xff) == 0)
                .build();
        assertThat(cpuStructure.registers().A())
                .withFailMessage("Expecting 0x%x + 0x%x to be equal to 0x%x\nBut was 0x%x\n".formatted(a, b, expected_bcd, cpuStructure.registers().A()))
                .isEqualTo((byte) expected_bcd);
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    @ParameterizedTest
    @MethodSource("getBytes")
    void givenTwoBytes_whenSubThenDecimalAdjust_thenResultIsDecimal(int a, int b) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withA(a)
                .withB(b)
                .build();

        Sub.sub_r8(Target.b).execute(cpuStructure);
        boolean carry_from_sub = cpuStructure.registers().getFlag(Flag.C);
        DecimalAdjustAcumulator.daa().execute(cpuStructure);

        int expected_bcd = decimalSub(a, b);
        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.C, expected_bcd >= 0x100 || carry_from_sub)
                .with(Flag.Z, (expected_bcd & 0xff) == 0)
                .with(Flag.H, false)
                .build();
        assertThat(cpuStructure.registers().A())
                .withFailMessage("Expecting 0x%x - 0x%x to be equal to 0x%x\nBut was 0x%x\n".formatted(a, b, expected_bcd, cpuStructure.registers().A()))
                .isEqualTo((byte) expected_bcd);
        assertFlagsMatch(expectedFlags, cpuStructure);
    }

    public int decimalAdd(int bcd_a, int bcd_b) {
        int a = Integer.parseInt("%x".formatted(bcd_a));
        int b = Integer.parseInt("%x".formatted(bcd_b));

        int result = a + b;

        //noinspection UnnecessaryLocalVariable
        int bcd_result = Integer.valueOf("%d".formatted(result), 16);
        return bcd_result;
    }

    public int decimalSub(int bcd_a, int bcd_b) {
        int a = Integer.parseInt("%x".formatted(bcd_a));
        int b = Integer.parseInt("%x".formatted(bcd_b));

        int result = (a - b + 100) % 100;

        //noinspection UnnecessaryLocalVariable
        int bcd_result = Integer.valueOf("%d".formatted(result), 16);
        return bcd_result;
    }

    @Test
    void givenAdditionModeAndValueGreaterThan99_whenDaa_thenSetsCarryFlag() {
        // This test catches the bug where carry flag is incorrectly set based on ALU overflow
        // The correct behavior is to set carry when value > 0x99 OR when C was already set
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x9A)
                .withUnsetFlags(Flag.N, Flag.C, Flag.H)  // Addition mode, no carry, no half carry
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0x00);  // 0x9A + 0x60 = 0xFA -> 0x00
        assertThat(cpu.registers().getFlag(Flag.C)).isTrue();
        assertThat(cpu.registers().getFlag(Flag.H)).isFalse();
    }
    
    @Test
    void givenValueAt99InAdditionMode_whenDaa_thenDoesNotSetCarryFlag() {
        // Edge case: 0x99 should NOT set carry (only > 0x99 should)
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x99)
                .withUnsetFlags(Flag.N, Flag.C, Flag.H)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0x99);  // No adjustment needed
        assertThat(cpu.registers().getFlag(Flag.C)).isFalse();  // Should NOT set carry
        assertThat(cpu.registers().getFlag(Flag.H)).isFalse();
    }
    
    @Test  
    void givenAdditionWithNoOverflow_whenDaa_thenCarryNotSetByAlu() {
        // This catches the bug: value 0x80 doesn't need upper nibble adjustment
        // but the buggy implementation might set carry based on ALU result
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x80)
                .withUnsetFlags(Flag.N, Flag.C, Flag.H)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0x80);  // No adjustment
        assertThat(cpu.registers().getFlag(Flag.C)).isFalse();  // Should NOT set carry
    }
    
    @Test
    void givenSubtractionModeWithNoCarry_whenDaa_thenCarryRemainsUnset() {
        // Critical test: In subtraction mode without initial carry,
        // carry should only be set if it was already set
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x50)
                .withSetFlags(Flag.N)  // Subtraction mode
                .withUnsetFlags(Flag.C, Flag.H)  // No carry initially
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0x50);  // No adjustment needed
        assertThat(cpu.registers().getFlag(Flag.C)).isFalse();  // Carry should remain unset
    }
    
    @Test
    void givenSubtractionModeWithZeroAndHalfCarry_whenDaaSubtracts_thenCarryRemainsUnset() {
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x00)
                .withSetFlags(Flag.N, Flag.H)  // Subtraction mode with half-carry
                .withUnsetFlags(Flag.C, Flag.Z)  // No initial carry
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0xFA);  // 0x00 - 0x06 = 0xFA
        assertThat(cpu.registers().getFlag(Flag.C))
            .withFailMessage("Carry flag should remain unset when DAA causes ALU borrow but C was not initially set")
            .isFalse();
    }

    @Test
    void givenCarryAlreadySet_whenDaa_thenCarryRemainsSet() {
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x50)
                .withSetFlags(Flag.C)
                .withUnsetFlags(Flag.N, Flag.H)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0xB0);  // 0x50 + 0x60 = 0xB0
        assertThat(cpu.registers().getFlag(Flag.C)).isTrue();
    }

    @Test
    void givenSubtractionModeWithCarry_whenDaa_thenCarryRemainsSet() {
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x50)
                .withSetFlags(Flag.N, Flag.C)   // Subtraction mode, Carry set (borrow occurred)
                .withUnsetFlags(Flag.H)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThatHex(cpu.registers().A()).isEqualTo((byte)0xF0);  // 0x50 - 0x60 = 0xF0
        assertThat(cpu.registers().getFlag(Flag.C)).isTrue();
    }

    @Test
    void givenHalfCarrySet_whenDaa_thenClearsHalfCarryFlag() {
        CpuStructure cpu = new CpuStructureBuilder()
                .withA(0x0C)
                .withSetFlags(Flag.H)
                .withUnsetFlags(Flag.N, Flag.C)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu);
        
        assertThat(cpu.registers().getFlag(Flag.H)).isFalse();
    }

    @Test
    void givenSubtractFlagState_whenDaa_thenPreservesSubtractFlag() {
        CpuStructure cpu1 = new CpuStructureBuilder()
                .withA(0x50)
                .withSetFlags(Flag.N)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu1);
        assertThat(cpu1.registers().getFlag(Flag.N)).isTrue();
        
        CpuStructure cpu2 = new CpuStructureBuilder()
                .withA(0x50)
                .withUnsetFlags(Flag.N)
                .build();
        
        DecimalAdjustAcumulator.daa().execute(cpu2);
        assertThat(cpu2.registers().getFlag(Flag.N)).isFalse();
    }
}