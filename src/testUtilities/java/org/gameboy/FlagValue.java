package org.gameboy;

import org.gameboy.cpu.Flag;

import java.util.Map.Entry;

public class FlagValue implements Entry<Flag, Boolean> {
    public final Flag flag;
    public boolean value;

    public FlagValue(Flag flag, boolean value) {
        this.flag = flag;
        this.value = value;
    }

    public static FlagValue setFlag(Flag flag) {
        return new FlagValue(flag, true);
    }

    public static FlagValue unsetFlag(Flag flag) {
        return new FlagValue(flag, false);
    }

    @Override
    public Flag getKey() {
        return flag;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public Boolean setValue(Boolean value) {
        this.value = value;
        return true;
    }

    @Override
    public int hashCode() {
        return this.getKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof @SuppressWarnings("rawtypes")Entry other) {
            return other.getKey().equals(this.getKey()) && other.getValue().equals(this.getValue());
        }

        return false;
    }
}
