package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.components.*;
import org.gameboy.Flag;
import org.gameboy.instructions.targets.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class JumpRelativeTest {
    @ParameterizedTest
    @ValueSource(bytes={(byte)0, (byte) 1, (byte) 7, (byte) -1, (byte) -54, (byte) 0xff})
    void givenImmediateByte_whenJumpRelativeWithoutCondition_thenPcCorrect(byte jump) {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(jump)
                .build();

        JumpRelative.jr().execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1 + jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotZero_whenJumpRelativeOnZero_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withExclusivelyUnsetFlags(Flag.Z)
                .withImm8(8)
                .build();

        JumpRelative.jr_cc(Condition.Z).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenZero_whenJumpRelativeOnZero_thenJump() {
        short initialPC = (short) 0x00ff;
        byte jump = 8;

        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z)
                .withPC(initialPC)
                .withImm8(jump)
                .build();

        JumpRelative.jr_cc(Condition.Z).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1 + jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenNotCarry_whenJumpRelativeOnCarry_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(8)
                .withExclusivelyUnsetFlags(Flag.C)
                .build();

        JumpRelative.jr_cc(Condition.C).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenCarry_whenJumpRelativeOnCarry_thenJump() {
        short initialPC = (short) 0x00ff;
        byte jump = 8;

        Instruction instruction = JumpRelative.jr_cc(Condition.C);
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(jump)
                .withExclusivelySetFlags(Flag.C)
                .build();

        instruction.execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1 + jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenZero_whenJumpRelativeOnNotZero_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(8)
                .withExclusivelySetFlags(Flag.Z)
                .build();

        JumpRelative.jr_cc(Condition.NZ).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotZero_whenJumpRelativeOnNotZero_thenJump() {
        short initialPC = (short) 0x00ff;
        byte jump = 8;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(jump)
                .withExclusivelyUnsetFlags(Flag.Z)
                .build();

        JumpRelative.jr_cc(Condition.NZ).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1 + jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenCarry_whenJumpRelativeOnNotCarry_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(8)
                .withExclusivelySetFlags(Flag.C)
                .build();

        JumpRelative.jr_cc(Condition.NC).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 1);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotCarry_whenJumpRelativeOnNotCarry_thenJump() {
        short initialPC = (short) 0x00ff;
        byte jump = 8;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm8(jump)
                .withExclusivelyUnsetFlags(Flag.C)
                .build();

        JumpRelative.jr_cc(Condition.NC).execute(cpuStructure);

        short expectedPC = (short) (initialPC + jump + 1);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }
}
