package org.gameboy.instructions;

import org.gameboy.components.*;
import org.gameboy.Flag;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class JumpRelativeTest {
    @ParameterizedTest
    @ValueSource(bytes={(byte)0, (byte) 1, (byte) 7, (byte) -1, (byte) -54, (byte) 0xff})
    void givenImmediateByte_whenJumpRelativeWithoutCondition_thenPcCorrect(byte jump) {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        Instruction instruction = JumpRelative.jr();
        Memory memory = new Memory();
        memory.write(initialPC, jump);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short expectedPC = (short) (initialPC + jump);
        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotZero_whenJumpRelativeOnZero_thenNoJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(false, Flag.Z);
        registers.setFlags(true, Flag.C, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.Z);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(initialPC);
    }

    @Test
    void givenZero_whenJumpRelativeOnZero_thenJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(true, Flag.Z);
        registers.setFlags(false, Flag.C, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.Z);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenNotCarry_whenJumpRelativeOnCarry_thenNoJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(false, Flag.C);
        registers.setFlags(true, Flag.Z, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.C);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(initialPC);
    }

    @Test
    void givenCarry_whenJumpRelativeOnCarry_thenJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(true, Flag.C);
        registers.setFlags(false, Flag.Z, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.C);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenZero_whenJumpRelativeOnNotZero_thenNoJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(true, Flag.Z);
        registers.setFlags(false, Flag.C, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.NZ);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(initialPC);
    }

    @Test
    void givenNotZero_whenJumpRelativeOnNotZero_thenJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(false, Flag.Z);
        registers.setFlags(true, Flag.C, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.NZ);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenCarry_whenJumpRelativeOnNotCarry_thenNoJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(true, Flag.C);
        registers.setFlags(false, Flag.Z, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.NC);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(initialPC);
    }

    @Test
    void givenNotCarry_whenJumpRelativeOnNotCarry_thenJump() {
        byte zero = (byte) 0;
        short initialPC = (short) 0x00ff;
        CpuRegisters registers = new CpuRegisters(zero, zero , zero, zero ,zero, initialPC, zero);
        registers.setFlags(false, Flag.C);
        registers.setFlags(true, Flag.Z, Flag.H, Flag.N);
        Instruction instruction = JumpRelative.jr_cc(Condition.NC);
        Memory memory = new Memory();
        memory.write(initialPC, (byte) 1);
        CpuStructure cpuStructure = new CpuStructure(registers, memory, new ArithmeticUnit(), new IncrementDecrementUnit());

        instruction.execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = registers.PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }
}
