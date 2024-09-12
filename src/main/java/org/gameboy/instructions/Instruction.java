package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public interface Instruction {
    String representation();

    void execute(CpuStructure cpuStructure);
}
