package org.gameboy.cpu.instructions;

import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.WordGeneralRegister;
import org.gameboy.cpu.instructions.targets.WordMemoryRegister;

import static org.gameboy.cpu.instructions.targets.OperationTarget.*;

public class BasicLoad implements Load {
    private final GenericOperationTarget destination;
    private final GenericOperationTarget source;

    private BasicLoad(GenericOperationTarget destination, GenericOperationTarget source) {
        this.destination = destination;
        this.source = source;
    }

    static BasicLoad ld_r8_r8(ByteRegister destination, ByteRegister source) {
        return new BasicLoad(destination.convert(), source.convert());
    }

    static BasicLoad ld_r8_imm8(ByteRegister destination) {
        return new BasicLoad(destination.convert(), IMM_8.direct());
    }

    static BasicLoad ld_r16_imm16(WordGeneralRegister register) {
        return new BasicLoad(register.convert(), IMM_16.direct());
    }

    static BasicLoad ld_A_mem16indirect(WordMemoryRegister indirectSource) {
        return new BasicLoad(A.direct(), indirectSource.convert());
    }

    static BasicLoad ld_mem16indirect_A(WordMemoryRegister indirectDestination) {
        return new BasicLoad(indirectDestination.convert(), A.direct());
    }

    static BasicLoad ld_indirectC_A() {
        return new BasicLoad(C.indirect(), A.direct());
    }

    static BasicLoad ld_A_indirectC() {
        return new BasicLoad(A.direct(), C.indirect());
    }

    static BasicLoad ld_HL_SP_OFFSET() {
        return new BasicLoad(HL.direct(), SP_OFFSET.direct());
    }

    static BasicLoad ld_imm16indirect_A() {
        return new BasicLoad(IMM_16.indirect(), A.direct());
    }

    static BasicLoad ld_A_imm16indirect() {
        return new BasicLoad(A.direct(), IMM_16.indirect());
    }

    @Override
    public GenericOperationTarget source() {
        return this.source;
    }

    @Override
    public GenericOperationTarget destination() {
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