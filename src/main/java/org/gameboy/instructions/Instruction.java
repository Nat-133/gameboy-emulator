package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public interface Instruction {
    void execute(CpuStructure cpuStructure);

    String representation();

    default void postFetch(CpuStructure cpuStructure) {}

    default boolean handlesFetch() {
        return false;
    }
}
