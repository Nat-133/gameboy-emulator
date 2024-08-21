package org.gameboy;

import org.gameboy.ArithmeticUnit.ArithmeticResult;
import org.gameboy.ArithmeticUnit.FlagValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.ArithmeticUnit.FlagValue.setFlag;
import static org.gameboy.ArithmeticUnit.FlagValue.unsetFlag;
import static org.gameboy.Flag.*;
import static org.gameboy.utils.BitUtilities.uint;

class ArithmeticUnitTest {

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xa1, (byte) 0x0f, (byte) 0x14})
    void givenByte_whenInc_thenResultCorrect(byte value) {
        ArithmeticResult result = ArithmeticUnit.inc(value);

        int expected = uint(value) + 1;

        assertThat(result.result()).isEqualTo((byte) expected);
    }

    static Stream<Arguments> getIncValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, List.of(unsetFlag(Z), unsetFlag(N), unsetFlag(H))),
                Arguments.of((byte) 0x0f, List.of(unsetFlag(Z), unsetFlag(N), setFlag(H))),
                Arguments.of((byte) 0xff, List.of(setFlag(Z), unsetFlag(N), setFlag(H)))
        );
    }

    @ParameterizedTest(name="{0}")
    @MethodSource("getIncValues")
    void givenByte_whenInc_thenFlagsCorrect(byte value, List<FlagValue> expectedFlags) {
        ArithmeticResult result = ArithmeticUnit.inc(value);

        assertThat(result.flagChanges()).containsExactlyElementsOf(expectedFlags);
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xa1, (byte) 0x0f, (byte) 0x14})
    void givenByte_whenDec_thenResultCorrect(byte value) {
        ArithmeticResult result = ArithmeticUnit.dec(value);

        int expected = uint(value) - 1;

        assertThat(result.result()).isEqualTo((byte) expected);
    }

    static Stream<Arguments> getDecValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, List.of(unsetFlag(Z), setFlag(N), setFlag(H))),
                Arguments.of((byte) 0x0f, List.of(unsetFlag(Z), setFlag(N), unsetFlag(H))),
                Arguments.of((byte) 0xff, List.of(unsetFlag(Z), setFlag(N), unsetFlag(H))),
                Arguments.of((byte) 0xf0, List.of(unsetFlag(Z), setFlag(N), setFlag(H))),
                Arguments.of((byte) 0x01, List.of(setFlag(Z), setFlag(N), unsetFlag(H)))
        );
    }

    @ParameterizedTest(name="{0}")
    @MethodSource("getDecValues")
    void givenByte_whenDec_thenFlagsCorrect(byte value, List<FlagValue> expectedFlags) {
        ArithmeticResult result = ArithmeticUnit.dec(value);

        assertThat(result.flagChanges()).containsExactlyElementsOf(expectedFlags);
    }
}