package org.gameboy.cpu.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.cpu.utils.BitUtilities.*;

class BitUtilitiesTest {
    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0x00, (byte) 0xff, (byte) 0xaf, (byte) 0x67, (byte) 0x14})
    void givenByte_whenGetAllBits_thenSame(byte value) {
        int result = bit_range(7, 0, value);

        assertThat((byte) result).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(shorts = {(short) 0x0000, (short) 0xff00, (short) 0xa0f0, (short) 0x0607, (short) 0x1145})
    void givenShort_whenGetAllBits_thenSame(short value) {
        int result = bit_range(15, 0, value);

        assertThat((short) result).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void givenAllOnesByte_whenGetThreeBits_thenValueIsCorrect(int offset) {
        byte val = (byte) 0xFF;

        int result = bit_range(offset + 2, offset, val);
        int expectedResult = 0b111;

        assertThat(result).isEqualTo(expectedResult);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13})
    void givenAllOnesShort_whenGetThreeBits_thenValueIsCorrect(int offset) {
        short val = (short) 0xFFFF;

        int result = bit_range(offset + 2, offset, val);
        int expectedResult = 0b111;

        assertThat(result).isEqualTo(expectedResult);
    }


    @ParameterizedTest
    @ValueSource(ints = {0xAB, 0xFF, 0x00, 0x01})
    void givenShort_whenGetUpperByte_thenCorrect(int expectedUpperByte) {
        byte lowerByte = (byte) (expectedUpperByte * 3);
        short val = (short) ((expectedUpperByte << 8) + uint(lowerByte));

        int actualUpperByte = uint(upper_byte(val));

        assertThat(actualUpperByte).isEqualTo(expectedUpperByte);
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0xAB, (byte) 0xFF, (byte) 0x00, (byte) 0x01})
    void givenByte_whenSetUpperByte_thenResultCorrect(byte upperByte) {
        short original = (short) 0x0000;

        short result = set_upper_byte(original, upperByte);

        assertThat(upper_byte(result)).isEqualTo(upperByte);
        assertThat(lower_byte(result)).isEqualTo((byte) 0);
    }

    @ParameterizedTest
    @ValueSource(bytes = {(byte) 0xAB, (byte) 0xFF, (byte) 0x00, (byte) 0x01})
    void givenByte_whenSetLowerByte_thenResultCorrect(byte lowerByte) {
        short original = (short) 0x0000;

        short result = set_lower_byte(original, lowerByte);

        assertThat(lower_byte(result)).isEqualTo(lowerByte);
        assertThat(upper_byte(result)).isEqualTo((byte) 0);
    }

    static Stream<Arguments> getCarryTestValues() {
        return Stream.of(
                Arguments.of((byte) 0b0000_0000, (byte) 0b0000_0000, (byte) 0b0000_0000),
                Arguments.of((byte) 0b1111_1111, (byte) 0b1111_1111, (byte) 0b1111_1111),
                Arguments.of((byte) 0b1111_1111, (byte) 0b0000_0001, (byte) 0b1111_1111),
                Arguments.of((byte) 0b1110_1111, (byte) 0b0000_0001, (byte) 0b0000_1111),
                Arguments.of((byte) 0b1110_1111, (byte) 0b0100_0001, (byte) 0b1100_1111)
        );
    }

    @ParameterizedTest
    @MethodSource("getCarryTestValues")
    void givenTwoBytes_whenCalculateCarryBits_thenResultCorrect(byte a, byte b, byte expectedCarry) {
        byte res = (byte) (a+b);

        byte carry = BitUtilities.calculate_carry_from_add(a, b, res);

        assertThat(carry).isEqualTo(expectedCarry);
    }
}