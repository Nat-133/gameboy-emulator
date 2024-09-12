package org.gameboy.instructions;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.common.OperationTargetAccessor;
import org.gameboy.instructions.targets.*;

import static org.gameboy.instructions.targets.OperationTarget.*;

public class Load implements Instruction {
    private final GenericOperationTarget destination;
    private final GenericOperationTarget source;

    private Load(GenericOperationTarget destination, GenericOperationTarget source) {
        this.destination = destination;
        this.source = source;
    }

    public static Load ld_r8_r8(ByteRegister destination, ByteRegister source) {
        return new Load(destination.convert(), source.convert());
    }

    public static Load ld_r8_imm8(ByteRegister destination) {
        return new Load(destination.convert(), IMM_8.direct());
    }

    public static Load ld_imm16indirect_sp() {
        return new Load(IMM_16.indirect(), SP.direct());
    }

    public static Load ld_r16_imm16(WordGeneralRegister register) {
        return new Load(register.convert(), IMM_16.direct());
    }

    public static Load ld_A_mem16indirect(WordMemoryRegister indirectSource) {
        return new Load(A.direct(), indirectSource.convert());
    }

    public static Load ld_mem16indirect_A(WordMemoryRegister indirectDestination) {
        return new Load(indirectDestination.convert(), A.direct());
    }

    public static Load ld_indirectC_A() {
        return new Load(C.indirect(), A.direct());
    }

    public static Load ld_A_indirectC() {
        return new Load(A.direct(), C.indirect());
    }

    public static Instruction load_HL_SP_OFFSET() {
        return new Load(HL.direct(), SP_OFFSET.direct());
    }

    public static Instruction ld_imm16indirect_A() {
        return new Load(IMM_16.indirect(), A.direct());
    }

    public static Instruction ld_A_imm16indirect() {
        return new Load(A.direct(), IMM_16.indirect());
    }

    public static Instruction load_SL_HL() {
        return new Load(SP.direct(), HL.direct());
    }

    @Override
    public String representation() {
        return "LD " + this.destination.representation() + "," + this.source.representation();
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
}