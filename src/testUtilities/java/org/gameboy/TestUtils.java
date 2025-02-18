package org.gameboy;

import org.gameboy.cpu.Flag;
import org.gameboy.cpu.instructions.targets.Condition;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class TestUtils {
    public static Flag[] getConditionFlags(Condition condition, boolean pass) {
        boolean invert = switch (condition) {
            case NZ, NC -> pass;
            case Z, C -> !pass;
        };
        Flag conditionFlag = switch (condition) {
            case NZ, Z -> Flag.Z;
            case NC, C -> Flag.C;
        };

        return invert
                ? Arrays.stream(Flag.values())
                .filter(f -> f != conditionFlag)
                .toArray(Flag[]::new)
                : new Flag[]{conditionFlag};
    }

    public static void waitFor(Supplier<Boolean> condition) throws TimeoutException {
        Instant end = Instant.now().plusMillis(100);
        while (!condition.get()) {
            if (Instant.now().isAfter(end)) {
                throw new TimeoutException("Timed out waiting for condition.");
            }
            Thread.yield();
        }
    }
}
