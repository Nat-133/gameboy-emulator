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
    void givenR8_thenOrderCorrect() {
        List<Target.R8> r8Order = threeBitValues.map(Target.R8::lookup).toList();
        assertThat(r8Order).containsExactly(
                Target.b, Target.c, Target.d, Target.e, Target.h, Target.l, Target.indirect_hl, Target.a
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
        List<Target.R16> r16Order = twoBitValues.map(Target.R16::lookup).toList();
        assertThat(r16Order).containsExactly(Target.bc, Target.de, Target.hl, Target.sp);
    }

    @Test
    void givenMem16_thenOrderCorrect() {
        List<Target.Mem16> mem16Order = twoBitValues.map(Target.Mem16::lookup).toList();
        assertThat(mem16Order).containsExactly(
                Target.indirect_bc, Target.indirect_de, Target.indirect_hl_inc, Target.indirect_hl_dec
        );
    }

    @Test
    void givenStk16_thenOrderCorrect() {
        List<Target.Stk16> stk16Order = twoBitValues.map(Target.Stk16::lookup).toList();
        assertThat(stk16Order).containsExactly(Target.bc, Target.de, Target.hl, Target.af);
    }
}
