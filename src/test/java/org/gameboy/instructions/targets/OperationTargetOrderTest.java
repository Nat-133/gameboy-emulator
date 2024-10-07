package org.gameboy.instructions.targets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class OperationTargetOrderTest {
    @Test
    void givenByteRegister_thenOrderCorrect() {
        assertThat(ByteRegister.values()).containsExactly(
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
        assertThat(Condition.values()).containsExactly(
                Condition.NZ,
                Condition.Z,
                Condition.NC,
                Condition.C
        );
    }

    @Test
    void givenWordGeneralRegister_thenOrderCorrect() {
        assertThat(WordGeneralRegister.values()).containsExactly(
                WordGeneralRegister.BC,
                WordGeneralRegister.DE,
                WordGeneralRegister.HL,
                WordGeneralRegister.SP
        );
    }

    @Test
    void givenWordMemoryRegister_thenOrderCorrect() {
        assertThat(WordMemoryRegister.values()).containsExactly(
                WordMemoryRegister.BC,
                WordMemoryRegister.DE,
                WordMemoryRegister.HL_INC,
                WordMemoryRegister.HL_DEC
        );
    }

    @Test
    void givenWordStackRegister_thenOrderCorrect() {
        assertThat(WordStackRegister.values()).containsExactly(
                WordStackRegister.BC,
                WordStackRegister.DE,
                WordStackRegister.HL,
                WordStackRegister.AF
        );
    }
}