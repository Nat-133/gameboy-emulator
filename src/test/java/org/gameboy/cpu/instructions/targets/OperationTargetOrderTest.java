package org.gameboy.cpu.instructions.targets;

import static org.gameboy.cpu.instructions.targets.Target.*;

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
    void givenR8_thenOrderCorrect() {
        List<R8> r8Order = threeBitValues.map(R8::lookup).toList();
        assertThat(r8Order).containsExactly(
                b, c, d, e, h, l, indirect_hl, a
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
    void givenR16_thenOrderCorrect() {
        List<R16> r16Order = twoBitValues.map(R16::lookup).toList();
        assertThat(r16Order).containsExactly(bc, de, hl, sp);
    }

    @Test
    void givenMem16_thenOrderCorrect() {
        List<Mem16> mem16Order = twoBitValues.map(Mem16::lookup).toList();
        assertThat(mem16Order).containsExactly(
                indirect_bc, indirect_de, indirect_hl_inc, indirect_hl_dec
        );
    }

    @Test
    void givenStk16_thenOrderCorrect() {
        List<Stk16> stk16Order = twoBitValues.map(Stk16::lookup).toList();
        assertThat(stk16Order).containsExactly(bc, de, hl, af);
    }
}
