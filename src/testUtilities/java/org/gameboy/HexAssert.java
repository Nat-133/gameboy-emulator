package org.gameboy;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

public class HexAssert<T extends Number> {
    private final T actual;
    private final ObjectAssert<T> assertion;

    public HexAssert(T actual) {
        this.actual = actual;
        assertion = Assertions.assertThat(actual);
    }

    public void isEqualTo(Number expected) {
        assertion
                .withFailMessage("expecting:\n 0x%x\nto be equal to:\n 0x%x\nbut was not.".formatted(actual, expected))
                .isEqualTo(expected);
    }
}
