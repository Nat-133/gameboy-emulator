package org.gameboy.instructions;

import org.gameboy.Cpu;
import org.gameboy.CpuStructureBuilder;
import org.gameboy.Flag;
import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.targets.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.TestDecoderFactory.testDecoder;
import static org.gameboy.TestUtils.getConditionFlags;

@SuppressWarnings("SameParameterValue")
public class CpuCycleTest {
    private static final List<ByteRegister> DIRECT_BYTE_REGISTERS = List.of(
            ByteRegister.B,
            ByteRegister.C,
            ByteRegister.D,
            ByteRegister.E,
            ByteRegister.H,
            ByteRegister.L,
            ByteRegister.A
    );

    private static final List<WordGeneralRegister> WORD_GENERAL_REGISTERS = List.of(WordGeneralRegister.values());
    private static final List<WordMemoryRegister> WORD_MEMORY_REGISTERS = List.of(WordMemoryRegister.values());
    private static final List<WordStackRegister> WORD_STACK_REGISTERS = List.of(WordStackRegister.values());
    private static final List<Condition> CONDITIONS = List.of(Condition.values());

    static Stream<Arguments> getInstructionExpectations() {
        return Stream.of(
                generateTestCase(Nop::nop, 1),

                generateR8TestCases(And::and_r8, 1),
                generateTestCase(And::and_imm8, 2),

                generateTestCase(Dec::dec_r8, ByteRegister.INDIRECT_HL, 3),
                generateTestCases(Dec::dec_r8, DIRECT_BYTE_REGISTERS, 1),
                generateTestCases(Dec::dec_r16, WORD_GENERAL_REGISTERS, 1),

                generateTestCase(Inc::inc_r8, ByteRegister.INDIRECT_HL, 3),
                generateTestCases(Inc::inc_r8, DIRECT_BYTE_REGISTERS, 1),
                generateTestCases(Inc::inc_r16, WORD_GENERAL_REGISTERS, 1),

                generateR8TestCases(Sub::sub_r8, 1),
                generateTestCase(Sub::sub_a_imm8, 2),

                generateR8TestCases(SubWithCarry::sbc_a_r8, 1),

                generateConditionalTestCases(Jump::jp_cc_nn, 4, 3),
                generateTestCase(Jump::jp_nn, 4),
                generateTestCase(Jump::jp_HL, 1),

                generateTestCase(JumpRelative::jr, 3),
                generateConditionalTestCases(JumpRelative::jr_cc, 3, 2),

                generateTestCase(Load::load_SP_HL, 2),
                generateTestCases(Load::ld_r16_imm16, WORD_GENERAL_REGISTERS, 3),
                generateTestCase(Load::ld_A_indirectC, 2),
                generateTestCase(Load::ld_indirectC_A, 2),
                generateTestCase(Load::ld_imm16indirect_A, 4),
                generateTestCase(Load::ld_A_imm16indirect, 4),
                generateTestCases(Load::ld_A_mem16indirect, WORD_MEMORY_REGISTERS, 2),
                generateTestCases(Load::ld_mem16indirect_A, WORD_MEMORY_REGISTERS, 2),
                generateR8TestCases(Load::ld_r8_imm8, 2),
                generateTestCase(Load::ld_imm16indirect_sp, 5),
                generateTestCase(Load::ld_HL_SP_OFFSET, 3),
                generateLdR8R8TestCases(),

                generateTestCase(LoadHigher::ldh_A_imm8, 3),
                generateTestCase(LoadHigher::ldh_imm8_A, 3),

                generateTestCase(Halt::HALT, 1),

                generateTestCase(Nop::nop, 1),

                generateR8TestCases(Compare::cp_r8, 1),
                generateTestCase(Compare::cp_imm8, 2),

                generateR8TestCases(Or::or_r8, 1),
                generateTestCase(Or::or_imm8, 2),

                generateR8TestCases(Xor::xor_r8, 1),
                generateTestCase(Xor::xor_imm8, 2),

                generateR8TestCases(Add::add_a_r8, 1),
                generateTestCase(Add::add_a_imm8, 2),
                generateTestCases(Add::add_hl_r16, WORD_GENERAL_REGISTERS, 2),
                generateTestCase(Add::add_sp_e8, 4),

                generateTestCase(RotateLeftCircular::rlca, 1),

                generateTestCase(RotateLeft::rla, 1),

                generateTestCase(RotateRightCircular::rrca, 1),

                generateTestCase(RotateRight::rra, 1),

                generateTestCase(Compliment::cpl, 1),

                generateTestCase(DecimalAdjustAcumulator::daa, 1),

                generateTestCase(SetCarryFlag::scf, 1),

                generateTestCase(ComplimentCarryFlag::ccf, 1),

                generateTestCases(Pop::pop_stk16, WORD_STACK_REGISTERS, 3),

                generateTestCases(Push::push_stk16, WORD_STACK_REGISTERS, 4),

                generateTestCase(Return::ret, 4),
                generateConditionalTestCases(ConditionalReturn::ret_cc, 5, 2),

                generateTestCase(Call::call, 6),
                generateConditionalTestCases(Call::call_cc, 6, 3),

                generateTestCase(Restart::rst_08H, 4),
                generateTestCase(Restart::rst_18H, 4),
                generateTestCase(Restart::rst_28H, 4),
                generateTestCase(Restart::rst_38H, 4),

                generateTestCase(EnableInterrupts::ei, 1),
                generateTestCase(DisableInterrupts::di, 1),

                generateTestCase(ReturnFromInterruptHandler::reti, 4)
        ).flatMap(x -> x);
    }

    @ParameterizedTest
    @MethodSource("getInstructionExpectations")
    void givenInstruction_whenRunOnCpu_thenClockCorrect(Instruction instruction, int expectedCycles, Flag... setFlags) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withExclusivelySetFlags(setFlags)
                .withDecoder(testDecoder(instruction))
                .build();
        Cpu cpu = new Cpu(cpuStructure);

        cpu.cycle();

        long actualCycles = cpuStructure.clock().getTime();
        assertThat(actualCycles).withFailMessage(getFailMessage(instruction, expectedCycles, actualCycles, setFlags)).isEqualTo(expectedCycles);
    }

    private String getFailMessage(Instruction instruction, int expectedCycles, long actualCycles, Flag[] setFlags) {
        return """
                Expecting:
                %s
                """.formatted(instruction.representation()) + (setFlags.length == 0 ? "" :
                """
                        with flags:
                        %s
                        """.formatted(Arrays.stream(setFlags)
                        .map(Enum::toString)
                        .collect(Collectors.joining(", ")))) +
                (expectedCycles > 1 ? "To take %s cycles, but took %s\n" : "To take %s cycle, but took %s\n").formatted(expectedCycles, actualCycles);
    }

    private static Stream<Arguments> generateLdR8R8TestCases() {
        return Stream.concat(
                DIRECT_BYTE_REGISTERS.stream().flatMap(r8 -> generateR8TestCases(r -> Load.ld_r8_r8(r, r8), 1)),
                generateTestCases(r -> Load.ld_r8_r8(ByteRegister.INDIRECT_HL, r), DIRECT_BYTE_REGISTERS, 2)
        );
    }

    private static Stream<Arguments> generateConditionalTestCases(Function<Condition, Instruction> constructor, int expectedCyclesWithCondition, int expectedCyclesWithoutCondition) {
        return CONDITIONS.stream()
                .flatMap(cc -> generateConditionalTestCases(
                        constructor,
                        cc,
                        expectedCyclesWithCondition,
                        expectedCyclesWithoutCondition));
    }

    private static Stream<Arguments> generateConditionalTestCases(
            Function<Condition, Instruction> constructor,
            Condition condition,
            int expectedCyclesWithCondition,
            int expectedCyclesWithoutCondition) {
        return Stream.of(
                Arguments.of(constructor.apply(condition), expectedCyclesWithCondition, getConditionFlags(condition, true)),
                Arguments.of(constructor.apply(condition), expectedCyclesWithoutCondition, getConditionFlags(condition, false))
        );
    }

    static Stream<Arguments> generateR8TestCases(Function<ByteRegister, Instruction> constructor, int expectedCyclesNoMemoryLoad) {
        return Stream.of(
                generateTestCases(constructor, CpuCycleTest.DIRECT_BYTE_REGISTERS, expectedCyclesNoMemoryLoad),
                generateTestCase(constructor, ByteRegister.INDIRECT_HL, expectedCyclesNoMemoryLoad + 1)
        ).flatMap(x -> x);
    }

    static <T> Stream<Arguments> generateTestCases(
            Function<T, Instruction> constructor,
            List<T> parameters,
            int expectedCycles) {
        return parameters.stream()
                .map(constructor)
                .map(instruction -> Arguments.of(instruction, expectedCycles, new Flag[]{}));
    }

    static <T> Stream<Arguments> generateTestCase(
            Function<T, Instruction> constructor,
            T parameter,
            int expectedCycles) {
        return generateTestCases(constructor, List.of(parameter), expectedCycles);
    }

    static Stream<Arguments> generateTestCase(
            Supplier<Instruction> constructor,
            int expectedCycles) {
        return Stream.of(Arguments.of(constructor.get(), expectedCycles, new Flag[]{}));
    }
}
