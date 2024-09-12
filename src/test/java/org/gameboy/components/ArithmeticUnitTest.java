package org.gameboy.components;

import org.gameboy.Flag;
import org.gameboy.components.ArithmeticUnit.ArithmeticResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map.Entry;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.FlagValue.setFlag;
import static org.gameboy.FlagValue.unsetFlag;
import static org.gameboy.Flag.*;
import static org.gameboy.utils.BitUtilities.uint;

class ArithmeticUnitTest {
    private ArithmeticUnit alu;

    @BeforeEach
    void setUp() {
        alu = new ArithmeticUnit();
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xa1, (byte) 0x0f, (byte) 0x14})
    void givenByte_whenInc_thenResultCorrect(byte value) {
        ArithmeticResult result = alu.inc(value);

        int expected = uint(value) + 1;

        assertThat(result.result()).isEqualTo((byte) expected);
    }

    static Stream<Arguments> getIncValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, new Entry[] {unsetFlag(Z), unsetFlag(N), unsetFlag(H)}),
                Arguments.of((byte) 0x0f, new Entry[] {unsetFlag(Z), unsetFlag(N), setFlag(H)}),
                Arguments.of((byte) 0xff, new Entry[] {setFlag(Z), unsetFlag(N), setFlag(H)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name="{0}")
    @MethodSource("getIncValues")
    final void givenByte_whenInc_thenFlagsCorrect(byte value, Entry<Flag, Boolean>... expectedFlags) {
        ArithmeticResult result = alu.inc(value);

        assertThat(result.flagChanges()).containsOnly(expectedFlags);
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xa1, (byte) 0x0f, (byte) 0x14})
    void givenByte_whenDec_thenResultCorrect(byte value) {
        ArithmeticResult result = alu.dec(value);

        int expected = uint(value) - 1;

        assertThat(result.result()).isEqualTo((byte) expected);
    }

    static Stream<Arguments> getDecValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, new Entry[] {unsetFlag(Z), setFlag(N), setFlag(H)}),
                Arguments.of((byte) 0x0f, new Entry[] {unsetFlag(Z), setFlag(N), unsetFlag(H)}),
                Arguments.of((byte) 0xff, new Entry[] {unsetFlag(Z), setFlag(N), unsetFlag(H)}),
                Arguments.of((byte) 0xf0, new Entry[] {unsetFlag(Z), setFlag(N), setFlag(H)}),
                Arguments.of((byte) 0x01, new Entry[] {setFlag(Z), setFlag(N), unsetFlag(H)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name="{0}")
    @MethodSource("getDecValues")
    final void givenByte_whenDec_thenFlagsCorrect(byte value, Entry<Flag, Boolean>... expectedFlags) {
        ArithmeticResult result = alu.dec(value);

        assertThat(result.flagChanges()).containsOnly(expectedFlags);
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

        ArithmeticResult result = alu.add(a, b);

        assertThat(result.result()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenSub_thenResultCorrect(byte a, byte b) {
        byte expectedResult = (byte) (uint(a) - uint(b));

        ArithmeticResult result = alu.sub(a, b);

        assertThat(result.result()).isEqualTo(expectedResult);
    }

    static Stream<Arguments> getAddValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, (byte) 0x00, new Entry[] {setFlag(Z), unsetFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0x0f, (byte) 0x0f, new Entry[] {unsetFlag(Z), unsetFlag(N), setFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xff, (byte) 0xff, new Entry[] {unsetFlag(Z), unsetFlag(N), setFlag(H), setFlag(C)}),
                Arguments.of((byte) 0xa5, (byte) 0x67, new Entry[] {unsetFlag(Z), unsetFlag(N), unsetFlag(H), setFlag(C)}),
                Arguments.of((byte) 0xc1, (byte) 0x11, new Entry[] {unsetFlag(Z), unsetFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xa3, (byte) 0x5d, new Entry[] {setFlag(Z), unsetFlag(N), setFlag(H), setFlag(C)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name="{0}+{1}")
    @MethodSource("getAddValues")
    final void givenTwoBytes_whenAdd_thenFlagsCorrect(byte a, byte b, Entry<Flag, Boolean>... expectedFlags) {
        ArithmeticResult result = alu.add(a, b);

        assertThat(result.flagChanges()).containsOnly(expectedFlags);
    }



    static Stream<Arguments> getSubValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, (byte) 0x00, new Entry[] {setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0x0f, (byte) 0x0f, new Entry[] {setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xff, (byte) 0xff, new Entry[] {setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xa5, (byte) 0x67, new Entry[] {unsetFlag(Z), setFlag(N), setFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xc1, (byte) 0x11, new Entry[] {unsetFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0x30, (byte) 0xa0, new Entry[] {unsetFlag(Z), setFlag(N), unsetFlag(H), setFlag(C)}),
                Arguments.of((byte) 0x32, (byte) 0xab, new Entry[] {unsetFlag(Z), setFlag(N), setFlag(H), setFlag(C)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name="{0}-{1}")
    @MethodSource("getSubValues")
    final void givenTwoBytes_whenSub_thenFlagsCorrect(byte a, byte b, Entry<Flag, Boolean>... expectedFlags) {
        ArithmeticResult result = alu.sub(a, b);

        assertThat(result.flagChanges()).containsOnly(expectedFlags);
    }
}