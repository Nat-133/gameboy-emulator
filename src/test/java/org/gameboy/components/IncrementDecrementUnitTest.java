package org.gameboy.components;

import org.junit.jupiter.api.Test;

import static org.gameboy.GameboyAssertions.assertThatHex;

class IncrementDecrementUnitTest {
    @Test
    void givenNextIncrementNotDisabled_whenIncrement_thenValueCorrect() {
        IncrementDecrementUnit idu = new IncrementDecrementUnit();

        short result = idu.increment((short) 0xfaab);

        assertThatHex(result).isEqualTo((short) 0xfaac);
    }

    @Test
    void givenNextIncrementDisabled_whenIncrement_thenValueUnchanged() {
        short initialValue = (short) 0xfaab;
        IncrementDecrementUnit idu = new IncrementDecrementUnit();
        idu.disableNextIncrement();

        short result = idu.increment(initialValue);

        assertThatHex(result).isEqualTo(initialValue);
    }

    @Test
    void givenNextIncrementDisabled_whenIncrementTwice_thenValueIncrementedOnce() {
        IncrementDecrementUnit idu = new IncrementDecrementUnit();
        idu.disableNextIncrement();

        short result = idu.increment((short) 0xfaab);
        result = idu.increment(result);

        assertThatHex(result).isEqualTo((short) 0xfaac);
    }
}