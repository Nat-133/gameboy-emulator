package org.gameboy;

import java.util.Hashtable;

import static org.gameboy.Flag.*;

public class FlagChangesetBuilder {
    private final Hashtable<Flag, Boolean> changes;

    public FlagChangesetBuilder() {
        this(new Hashtable<>(4, 1f));
    }

    public FlagChangesetBuilder(boolean initalFlagValues) {
        this();
        this.withAll(initalFlagValues);
    }

    public FlagChangesetBuilder(Hashtable<Flag, Boolean> changes) {


        this.changes = new Hashtable<>(changes);
    }

    public FlagChangesetBuilder with(Flag flag, boolean value) {
        changes.put(flag, value);
        return this;
    }

    public FlagChangesetBuilder without(Flag flag) {
        changes.remove(flag);
        return this;
    }

    public Hashtable<Flag, Boolean> build() {
        return changes;
    }

    public FlagChangesetBuilder withAll(boolean value) {
        return this.with(Z, value)
                .with(H, value)
                .with(C, value)
                .with(N, value);
    }
}
