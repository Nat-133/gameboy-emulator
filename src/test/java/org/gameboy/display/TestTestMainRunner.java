package org.gameboy.display;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestTestMainRunner {
    @Test
    @Disabled("Manual test - runs the emulator")
    public void runTestMain() throws Exception {
        TestMain.main(new String[]{});
    }
}