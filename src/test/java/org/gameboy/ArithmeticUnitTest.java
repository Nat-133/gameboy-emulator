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

    static Stream<Arguments> getBasicOperationValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, (byte) 0x00),
                Arguments.of((byte) 0x00, (byte) 0xff),
                Arguments.of((byte) 0xf0, (byte) 0x0f),
                Arguments.of((byte) 0xf0, (byte) 0x1f),
                Arguments.of((byte) 0xf0, (byte) 0xee),
                Arguments.of((byte) 0xef, (byte) 0xa6)
        );
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenAdd_thenResultCorrect(byte a, byte b) {
        byte expectedResult = (byte) (a+b);

        ArithmeticResult result = ArithmeticUnit.add(a, b);

        assertThat(result.result()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenSub_thenResultCorrect(byte a, byte b) {
        byte expectedResult = (byte) (uint(a) - uint(b));

        ArithmeticResult result = ArithmeticUnit.sub(a, b);

        assertThat(result.result()).isEqualTo(expectedResult);
    }

    static Stream<Arguments> getAddValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, (byte) 0x00, List.of(setFlag(Z), unsetFlag(N), unsetFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0x0f, (byte) 0x0f, List.of(unsetFlag(Z), unsetFlag(N), setFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0xff, (byte) 0xff, List.of(unsetFlag(Z), unsetFlag(N), setFlag(H), setFlag(C))),
                Arguments.of((byte) 0xa5, (byte) 0x67, List.of(unsetFlag(Z), unsetFlag(N), unsetFlag(H), setFlag(C))),
                Arguments.of((byte) 0xc1, (byte) 0x11, List.of(unsetFlag(Z), unsetFlag(N), unsetFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0xa3, (byte) 0x5d, List.of(setFlag(Z), unsetFlag(N), setFlag(H), setFlag(C)))
        );
    }

    @ParameterizedTest(name="{0}+{1}")
    @MethodSource("getAddValues")
    void givenTwoBytes_whenAdd_thenFlagsCorrect(byte a, byte b, List<FlagValue> expectedFlags) {
        ArithmeticResult result = ArithmeticUnit.add(a, b);

        assertThat(result.flagChanges()).containsExactlyElementsOf(expectedFlags);
    }



    static Stream<Arguments> getSubValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, (byte) 0x00, List.of(setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0x0f, (byte) 0x0f, List.of(setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0xff, (byte) 0xff, List.of(setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0xa5, (byte) 0x67, List.of(unsetFlag(Z), setFlag(N), setFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0xc1, (byte) 0x11, List.of(unsetFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C))),
                Arguments.of((byte) 0x30, (byte) 0xa0, List.of(unsetFlag(Z), setFlag(N), unsetFlag(H), setFlag(C))),
                Arguments.of((byte) 0x32, (byte) 0xab, List.of(unsetFlag(Z), setFlag(N), setFlag(H), setFlag(C)))
        );
    }

    @ParameterizedTest(name="{0}-{1}")
    @MethodSource("getSubValues")
    void givenTwoBytes_whenSub_thenFlagsCorrect(byte a, byte b, List<FlagValue> expectedFlags) {
        ArithmeticResult result = ArithmeticUnit.sub(a, b);

        assertThat(result.flagChanges()).containsExactlyElementsOf(expectedFlags);
    }
}