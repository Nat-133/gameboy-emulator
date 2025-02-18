package org.gameboy.cpu.components;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.FlagValue.setFlag;
import static org.gameboy.FlagValue.unsetFlag;
import static org.gameboy.GameboyAssertions.assertFlagsMatch;
import static org.gameboy.cpu.Flag.*;
import static org.gameboy.cpu.utils.BitUtilities.uint;

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
                Arguments.of((byte) 0x00, new Entry[]{unsetFlag(Z), unsetFlag(N), unsetFlag(H)}),
                Arguments.of((byte) 0x0f, new Entry[]{unsetFlag(Z), unsetFlag(N), setFlag(H)}),
                Arguments.of((byte) 0xff, new Entry[]{setFlag(Z), unsetFlag(N), setFlag(H)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name = "{0}")
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
                Arguments.of((byte) 0x00, new Entry[]{unsetFlag(Z), setFlag(N), setFlag(H)}),
                Arguments.of((byte) 0x0f, new Entry[]{unsetFlag(Z), setFlag(N), unsetFlag(H)}),
                Arguments.of((byte) 0xff, new Entry[]{unsetFlag(Z), setFlag(N), unsetFlag(H)}),
                Arguments.of((byte) 0xf0, new Entry[]{unsetFlag(Z), setFlag(N), setFlag(H)}),
                Arguments.of((byte) 0x01, new Entry[]{setFlag(Z), setFlag(N), unsetFlag(H)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name = "{0}")
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
        byte expectedResult = (byte) (a + b);

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
                Arguments.of((byte) 0x00, (byte) 0x00, new Entry[]{setFlag(Z), unsetFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0x0f, (byte) 0x0f, new Entry[]{unsetFlag(Z), unsetFlag(N), setFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xff, (byte) 0xff, new Entry[]{unsetFlag(Z), unsetFlag(N), setFlag(H), setFlag(C)}),
                Arguments.of((byte) 0xa5, (byte) 0x67, new Entry[]{unsetFlag(Z), unsetFlag(N), unsetFlag(H), setFlag(C)}),
                Arguments.of((byte) 0xc1, (byte) 0x11, new Entry[]{unsetFlag(Z), unsetFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xa3, (byte) 0x5d, new Entry[]{setFlag(Z), unsetFlag(N), setFlag(H), setFlag(C)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name = "{0}+{1}")
    @MethodSource("getAddValues")
    final void givenTwoBytes_whenAdd_thenFlagsCorrect(byte a, byte b, Entry<Flag, Boolean>... expectedFlags) {
        ArithmeticResult result = alu.add(a, b);

        assertThat(result.flagChanges()).containsOnly(expectedFlags);
    }


    static Stream<Arguments> getSubValues() {
        return Stream.of(
                Arguments.of((byte) 0x00, (byte) 0x00, new Entry[]{setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0x0f, (byte) 0x0f, new Entry[]{setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xff, (byte) 0xff, new Entry[]{setFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xa5, (byte) 0x67, new Entry[]{unsetFlag(Z), setFlag(N), setFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0xc1, (byte) 0x11, new Entry[]{unsetFlag(Z), setFlag(N), unsetFlag(H), unsetFlag(C)}),
                Arguments.of((byte) 0x30, (byte) 0xa0, new Entry[]{unsetFlag(Z), setFlag(N), unsetFlag(H), setFlag(C)}),
                Arguments.of((byte) 0x32, (byte) 0xab, new Entry[]{unsetFlag(Z), setFlag(N), setFlag(H), setFlag(C)}),
                Arguments.of((byte) 0x55, (byte) 0x65, new Entry[]{unsetFlag(Z), setFlag(N), unsetFlag(H), setFlag(C)})
        );
    }

    @SafeVarargs
    @ParameterizedTest(name = "{0}-{1}")
    @MethodSource("getSubValues")
    final void givenTwoBytes_whenSub_thenFlagsCorrect(byte a, byte b, Entry<Flag, Boolean>... expectedFlags) {
        ArithmeticResult result = alu.sub(a, b);

        assertThat(result.flagChanges()).containsOnly(expectedFlags);
    }

    @ParameterizedTest
    @CsvSource({"0x00, 0x73", "0xff, 0x43", "0xf1, 0x1f", "0x23, 0x23"})
    void givenTwoBytes_whenAnd_thenResultCorrect(int a, int b) {
        ArithmeticResult result = alu.and((byte) a, (byte) b);

        int expected = a & b;

        assertThat(result.result()).isEqualTo((byte) expected);
    }

    @ParameterizedTest
    @CsvSource({"0x00, 0x73", "0xff, 0x43", "0xf1, 0x1f", "0x23, 0x23"})
    void givenTwoBytes_whenAnd_thenFlagsCorrect(int a, int b) {
        ArithmeticResult result = alu.and((byte) a, (byte) b);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Z, a == 0 || b == 0)
                .with(H, true)
                .with(C, false)
                .with(N, false)
                .build();
        assertFlagsMatch(expectedFlags, result.flagChanges());
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenOr_thenResultCorrect(byte a, byte b) {
        byte expectedResult = (byte) (a | b);

        ArithmeticResult result = alu.or(a, b);

        assertThat(result.result()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenOr_thenFlagsCorrect(byte a, byte b) {

        ArithmeticResult result = alu.or(a, b);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Z, a == 0 && b == 0)
                .with(H, false)
                .with(C, false)
                .with(N, false)
                .build();
        assertFlagsMatch(expectedFlags, result.flagChanges());
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenXor_thenResultCorrect(byte a, byte b) {
        byte expectedResult = (byte) (a | b);

        ArithmeticResult result = alu.or(a, b);

        assertThat(result.result()).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @MethodSource("getBasicOperationValues")
    void givenTwoBytes_whenXor_thenFlagsCorrect(byte a, byte b) {

        ArithmeticResult result = alu.xor(a, b);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Z, a == 0 && b == 0)
                .with(H, false)
                .with(C, false)
                .with(N, false)
                .build();
        assertFlagsMatch(expectedFlags, result.flagChanges());
    }

    static Stream<Arguments> getRotateLeftCircularValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b10101010),
                Arguments.of(0b00000000, 0b00000000),
                Arguments.of(0b11111111, 0b11111111),
                Arguments.of(0b11101000, 0b11010001)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateLeftCircularValues")
    void givenByte_whenRotateLeftCircular_thenResultIsCorrect(int val, int expectedResult) {
        ArithmeticResult res = alu.rotate_left_circular((byte) val);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(C, (val & 0b1000_0000) != 0)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
        assertThat(res.result()).isEqualTo((byte) expectedResult);
    }

    static Stream<Arguments> getRotateRightCircularValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b10101010),
                Arguments.of(0b00000000, 0b00000000),
                Arguments.of(0b11111111, 0b11111111),
                Arguments.of(0b11101000, 0b01110100)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateRightCircularValues")
    void givenByte_whenRotateRightCircular_thenResultIsCorrect(int val, int expectedResult) {
        ArithmeticResult res = alu.rotate_right_circular((byte) val);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(C, (val & 0b1) == 1)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
        assertThat(res.result()).isEqualTo((byte) expectedResult);
    }

    static Stream<Arguments> getRotateLeftValues() {
        return Stream.of(
                Arguments.of(0b01010101, 1, 0b10101011),
                Arguments.of(0b00000000, 1, 0b00000001),
                Arguments.of(0b11111111, 0, 0b11111110),
                Arguments.of(0b11101000, 0, 0b11010000)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateLeftValues")
    void givenByte_whenRotateLeft_thenResultIsCorrect(int val, int carry, int expectedResult) {
        ArithmeticResult res = alu.rotate_left((byte) val, carry == 1);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(C, (val & 0b1000_0000) != 0)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
        assertThat(res.result()).isEqualTo((byte) expectedResult);
    }

    static Stream<Arguments> getRotateRightValues() {
        return Stream.of(
                Arguments.of(0b01010101, 1, 0b10101010),
                Arguments.of(0b00000000, 1, 0b10000000),
                Arguments.of(0b11111111, 0, 0b01111111),
                Arguments.of(0b11101000, 0, 0b01110100)
        );
    }

    @ParameterizedTest
    @MethodSource("getRotateRightValues")
    void givenByte_whenRotateRight_thenResultIsCorrect(int val, int carry, int expectedResult) {
        ArithmeticResult res = alu.rotate_right((byte) val, carry==1);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .withAll(false)
                .with(C, (val & 0b1) == 1)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
        assertThat(res.result()).isEqualTo((byte) expectedResult);
    }

    static Stream<Arguments> getComplimentaryValues() {
        return Stream.of(
                Arguments.of(0b01010101, 0b10101010),
                Arguments.of(0b00000000, 0b11111111),
                Arguments.of(0b11111111, 0b00000000),
                Arguments.of(0b11101000, 0b00010111)
        );
    }

    @ParameterizedTest
    @MethodSource("getComplimentaryValues")
    void givenByte_whenCompliment_thenResultIsCorrectAndFlagsCorrect(int val, int expectedResult) {
        ArithmeticResult res = alu.compliment((byte) val);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.N, true)
                .with(Flag.H, true)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
        assertThat(res.result()).isEqualTo((byte) expectedResult);
    }

    @Test
    void givenSetCarryFlag_thenCorrectFlagsReturned() {
        ArithmeticResult res = alu.set_carry_flag();

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, true)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenComplimentCarryFlag_thenCorrectFlagsReturned(boolean carry_flag) {
        ArithmeticResult res = alu.compliment_carry_flag(carry_flag);

        Hashtable<Flag, Boolean> expectedFlags = new FlagChangesetBuilder()
                .with(Flag.N, false)
                .with(Flag.H, false)
                .with(Flag.C, !carry_flag)
                .build();
        assertFlagsMatch(expectedFlags, res.flagChanges());
    }
}