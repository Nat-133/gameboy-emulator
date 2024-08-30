package org.gameboy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailingTest {
    @Test
    public void givenMemorySave_whenMemoryLoad_thenCorrect() {
        assertTrue(false);
    }
}