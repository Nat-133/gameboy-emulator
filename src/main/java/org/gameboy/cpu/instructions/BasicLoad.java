package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.Target;

public class BasicLoad implements Load {
    private final Target destination;
    private final Target source;

    private BasicLoad(Target destination, Target source) {
        this.destination = destination;
        this.source = source;
    }

    static BasicLoad ld_r8_r8(Target.R8 destination, Target.R8 source) {
        return new BasicLoad(destination, source);
    }

    static BasicLoad ld_r8_imm8(Target.R8 destination) {
        return new BasicLoad(destination, Target.imm_8);
    }

    static BasicLoad ld_r16_imm16(Target.R16 register) {
        return new BasicLoad(register, Target.imm_16);
    }

    static BasicLoad ld_A_mem16indirect(Target.Mem16 indirectSource) {
        return new BasicLoad(Target.a, indirectSource);
    }

    static BasicLoad ld_mem16indirect_A(Target.Mem16 indirectDestination) {
        return new BasicLoad(indirectDestination, Target.a);
    }

    static BasicLoad ld_indirectC_A() {
        return new BasicLoad(Target.indirect_c, Target.a);
    }

    static BasicLoad ld_A_indirectC() {
        return new BasicLoad(Target.a, Target.indirect_c);
    }

    static BasicLoad ld_HL_SP_OFFSET() {
        return new BasicLoad(Target.hl, Target.sp_offset);
    }

    static BasicLoad ld_imm16indirect_A() {
        return new BasicLoad(Target.indirect_imm_16, Target.a);
    }

    static BasicLoad ld_A_imm16indirect() {
        return new BasicLoad(Target.a, Target.indirect_imm_16);
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
