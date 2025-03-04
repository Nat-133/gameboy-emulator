package org.gameboy.cpu.instructions.targets;

import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.gameboy.utils.MultiBitValue.TwoBitValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OperationTargetOrderTest {
    Stream<ThreeBitValue> threeBitValues = IntStream.range(0, 8).mapToObj(ThreeBitValue::from);
    Stream<TwoBitValue> twoBitValues = IntStream.range(0, 4).mapToObj(TwoBitValue::from);

    @Test
    void givenByteRegister_thenOrderCorrect() {
        List<ByteRegister> byteRegisterOrder = threeBitValues.map(ByteRegister::lookup).toList();
        assertThat(byteRegisterOrder).containsExactly(
                ByteRegister.B,
                ByteRegister.C,
                ByteRegister.D,
                ByteRegister.E,
                ByteRegister.H,
                ByteRegister.L,
                ByteRegister.INDIRECT_HL,
                ByteRegister.A
        );
    }

    @Test
    void givenCondition_thenOrderCorrect() {
        List<Condition> conditionOrder = twoBitValues.map(Condition::lookup).toList();
        assertThat(conditionOrder).containsExactly(
                Condition.NZ,
                Condition.Z,
                Condition.NC,
                Condition.C
        );
    }

    @Test
    void givenWordGeneralRegister_thenOrderCorrect() {
        List<WordGeneralRegister> wordGeneralRegisterOrder = twoBitValues.map(WordGeneralRegister::lookup).toList();
        assertThat(wordGeneralRegisterOrder).containsExactly(
                WordGeneralRegister.BC,
                WordGeneralRegister.DE,
                WordGeneralRegister.HL,
                WordGeneralRegister.SP
        );
    }

    @Test
    void givenWordMemoryRegister_thenOrderCorrect() {
        List<WordMemoryRegister> wordMemoryRegisterOrder = twoBitValues.map(WordMemoryRegister::lookup).toList();
        assertThat(wordMemoryRegisterOrder).containsExactly(
                WordMemoryRegister.BC,
                WordMemoryRegister.DE,
                WordMemoryRegister.HL_INC,
                WordMemoryRegister.HL_DEC
        );
    }

    @Test
    void givenWordStackRegister_thenOrderCorrect() {
        List<WordStackRegister> wordStackRegisterOrder = twoBitValues.map(WordStackRegister::lookup).toList();
        assertThat(wordStackRegisterOrder).containsExactly(
                WordStackRegister.BC,
                WordStackRegister.DE,
                WordStackRegister.HL,
                WordStackRegister.AF
        );
    }
}