package org.gameboy.instructions;

import org.gameboy.OperationTargetAccessor;
import org.gameboy.instructions.targets.ByteRegister;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.WordGeneralRegister;
import org.gameboy.instructions.targets.WordMemoryRegister;

import static org.gameboy.instructions.targets.GenericOperationTarget.*;

public class Load implements Instruction {
    private final GenericOperationTarget destination;
    private final GenericOperationTarget source;

    private Load(GenericOperationTarget destination, GenericOperationTarget source) {
        this.destination = destination;
        this.source = source;
    }

    public static Load load_register_register(ByteRegister destination, ByteRegister source) {
        return new Load(destination.convert(), source.convert());
    }

    public static Load load_register_indirect(ByteRegister destination, WordMemoryRegister indirectSource) {
        return new Load(destination.convert(), indirectSource.convert());
    }

    public static Load load_indirect_register(WordMemoryRegister indirectDestination, ByteRegister source) {
        return new Load(indirectDestination.convert(), source.convert());
    }

    public static Load load_register_imm8(ByteRegister destination) {
        return new Load(destination.convert(), IMM_8);
    }

    public static Load load_imm16indirect_sp() {
        return new Load(IMM_16_INDIRECT, SP);
    }

    public static Load load_wordGeneralRegister_imm16(WordGeneralRegister register) {
        return new Load(register.convert(), IMM_16);
    }

    @Override
    public String representation() {
        return "LD " + this.destination.representation() + "," + this.source.representation();
    }

    @Override
    public void execute(OperationTargetAccessor operationTargetAccessor) {
        short loaded_value = operationTargetAccessor.getValue(this.source);
        operationTargetAccessor.setValue(this.destination, loaded_value);
    }
}