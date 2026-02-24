package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

public class BasicLoad implements Load {
    private final Target destination;
    private final Target source;

    private BasicLoad(Target destination, Target source) {
        this.destination = destination;
        this.source = source;
    }

    static BasicLoad ld_r8_r8(R8 destination, R8 source) {
        return new BasicLoad((Target) destination, (Target) source);
    }

    static BasicLoad ld_r8_imm8(R8 destination) {
        return new BasicLoad((Target) destination, imm_8);
    }

    static BasicLoad ld_r16_imm16(R16 register) {
        return new BasicLoad((Target) register, imm_16);
    }

    static BasicLoad ld_A_mem16indirect(Mem16 indirectSource) {
        return new BasicLoad(a, (Target) indirectSource);
    }

    static BasicLoad ld_mem16indirect_A(Mem16 indirectDestination) {
        return new BasicLoad((Target) indirectDestination, a);
    }

    static BasicLoad ld_indirectC_A() {
        return new BasicLoad(indirect_c, a);
    }

    static BasicLoad ld_A_indirectC() {
        return new BasicLoad(a, indirect_c);
    }

    static BasicLoad ld_HL_SP_OFFSET() {
        return new BasicLoad(hl, sp_offset);
    }

    static BasicLoad ld_imm16indirect_A() {
        return new BasicLoad(indirect_imm_16, a);
    }

    static BasicLoad ld_A_imm16indirect() {
        return new BasicLoad(a, indirect_imm_16);
    }

    @Override
    public Target source() {
        return this.source;
    }

    @Override
    public Target destination() {
        return this.destination;
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        short loaded_value = operationTargetAccessor.getValue(this.source);
        operationTargetAccessor.setValue(this.destination, loaded_value);
    }

    @Override
    public String toString() {
        return representation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Instruction other) {
            return this.representation().equals(other.representation());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.representation().hashCode();
    }
}
