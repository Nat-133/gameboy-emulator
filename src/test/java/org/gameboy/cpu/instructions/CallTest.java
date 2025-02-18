package org.gameboy.cpu.instructions;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.targets.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.TestUtils.getConditionFlags;

class CallTest {
    @Test
    void givenWord_whenCall_thenProgramCounterAndStackPointerCorrect() {
        int sp = 0x4567;
        int pc = 0x8239;
        int functionPointer = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withSP(sp)
                .withPC(pc)
                .withImm16(functionPointer)
                .build();

        Call.call().execute(cpuStructure);

        short topOfStack = ControlFlow.popFromStack(cpuStructure);
        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp));
        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (functionPointer));
        assertThatHex(topOfStack).isEqualTo((short) (pc + 2));
    }

    @ParameterizedTest
    @EnumSource(Condition.class)
    void givenConditionSatisfied_whenCallOnCondition_thenProgramCounterAndStackPointerCorrect(Condition cc) {
        int sp = 0x4567;
        int pc = 0x8239;
        int functionPointer = 0x1234;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(getConditionFlags(cc, true))
                .withSP(sp)
                .withPC(pc)
                .withImm16(functionPointer)
                .build();

        Call.call_cc(cc).execute(cpuStructure);

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp - 2));
        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (functionPointer));
        short topOfStack = ControlFlow.popFromStack(cpuStructure);
        assertThatHex(topOfStack).isEqualTo((short) (pc + 2));
    }

    @ParameterizedTest
    @EnumSource(Condition.class)
    void givenConditionUnsatisfied_whenCallOnCondition_thenProgramCounterAndStackPointerCorrect(Condition cc) {
        int sp = 0x4567;
        int pc = 0x8239;
        int functionPointer = 0x1234;
        int stack = 0xabcd;
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(getConditionFlags(cc, false))
                .withSP(sp)
                .withPC(pc)
                .withStack(stack)
                .withImm16(functionPointer)
                .build();

        Call.call_cc(cc).execute(cpuStructure);

        assertThatHex(cpuStructure.registers().SP()).isEqualTo((short) (sp));
        assertThatHex(cpuStructure.registers().PC()).isEqualTo((short) (pc + 2));
        short topOfStack = ControlFlow.popFromStack(cpuStructure);
        assertThatHex(topOfStack).isEqualTo((short) stack);
    }
}