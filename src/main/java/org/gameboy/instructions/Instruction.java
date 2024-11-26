package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;

public interface Instruction {
    void execute(CpuStructure cpuStructure);

    String representation();
}
