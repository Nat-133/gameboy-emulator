package org.gameboy;

import org.gameboy.instructions.targets.Condition;

import java.util.Arrays;

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
}
