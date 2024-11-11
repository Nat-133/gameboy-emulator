package org.gameboy;

import java.util.Hashtable;

public record ArithmeticResult(byte result, Hashtable<Flag, Boolean> flagChanges) {
}
