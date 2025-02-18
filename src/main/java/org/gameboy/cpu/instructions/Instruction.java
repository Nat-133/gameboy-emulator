package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;

public interface Instruction {
    void execute(CpuStructure cpuStructure);

    String representation();

    default void postFetch(CpuStructure cpuStructure) {}

    default boolean handlesFetch() {
        return false;
    }
}
