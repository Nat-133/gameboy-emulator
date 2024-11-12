package org.gameboy.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.targets.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JumpTest {
    @ParameterizedTest
    @ValueSource(ints={0x0000,  0x0001,  0x1234,  0xffff,  0x0163,  0x00ff})
    void givenImm16_whenJumpWithoutCondition_thenPcCorrect(int address) {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(address)
                .build();

        Jump.jp_nn().execute(cpuStructure);

        short expectedPC = (short) (address);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotZero_whenJumpOnZero_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withExclusivelyUnsetFlags(Flag.Z)
                .withImm16(0x1234)
                .build();

        Jump.jp_cc_nn(Condition.Z).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 2);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenZero_whenJumpOnZero_thenJump() {
        short initialPC = (short) 0x00ff;
        int jump = 0x1234;

        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(Flag.Z)
                .withPC(initialPC)
                .withImm16(jump)
                .build();

        Jump.jp_cc_nn(Condition.Z).execute(cpuStructure);

        short expectedPC = (short) (jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenNotCarry_whenJumpOnCarry_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(0x1234)
                .withExclusivelyUnsetFlags(Flag.C)
                .build();

        Jump.jp_cc_nn(Condition.C).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 2);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenCarry_whenJumpOnCarry_thenJump() {
        short initialPC = (short) 0x00ff;
        int jump = 0x1234;

        Instruction instruction = Jump.jp_cc_nn(Condition.C);
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(jump)
                .withExclusivelySetFlags(Flag.C)
                .build();

        instruction.execute(cpuStructure);

        short expectedPC = (short) (jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenZero_whenJumpOnNotZero_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(0x1234)
                .withExclusivelySetFlags(Flag.Z)
                .build();

        Jump.jp_cc_nn(Condition.NZ).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 2);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotZero_whenJumpOnNotZero_thenJump() {
        short initialPC = (short) 0x00ff;
        int jump = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(jump)
                .withExclusivelyUnsetFlags(Flag.Z)
                .build();

        Jump.jp_cc_nn(Condition.NZ).execute(cpuStructure);

        short expectedPC = (short) (jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenCarry_whenJumpOnNotCarry_thenNoJump() {
        short initialPC = (short) 0x00ff;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(0x1234)
                .withExclusivelySetFlags(Flag.C)
                .build();

        Jump.jp_cc_nn(Condition.NC).execute(cpuStructure);

        short expectedPC = (short) (initialPC + 2);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }

    @Test
    void givenNotCarry_whenJumpOnNotCarry_thenJump() {
        short initialPC = (short) 0x00ff;
        int jump = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(jump)
                .withExclusivelyUnsetFlags(Flag.C)
                .build();

        Jump.jp_cc_nn(Condition.NC).execute(cpuStructure);

        short expectedPC = (short) (jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }


    @Test
    void givenHl_whenJumpHl_thenCorrectResult() {
        short initialPC = (short) 0x00ff;
        int jump = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withPC(initialPC)
                .withImm16(jump)
                .withExclusivelyUnsetFlags(Flag.C)
                .build();

        Jump.jp_cc_nn(Condition.NC).execute(cpuStructure);

        short expectedPC = (short) (jump);
        short actualPC = cpuStructure.registers().PC();
        assertThat(actualPC).isEqualTo(expectedPC);
    }
}