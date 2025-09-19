package org.gameboy.cpu.instructions;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.common.ControlFlow;
import org.gameboy.cpu.instructions.common.OperationTargetAccessor;
import org.gameboy.cpu.instructions.targets.ByteRegister;
import org.gameboy.cpu.instructions.targets.GenericOperationTarget;
import org.gameboy.cpu.instructions.targets.OperationTarget;
import org.gameboy.cpu.instructions.targets.WordGeneralRegister;

import java.util.Hashtable;

import static org.gameboy.utils.BitUtilities.*;

public class Add implements Instruction {
    private final GenericOperationTarget left;
    private final GenericOperationTarget right;

    private Add(GenericOperationTarget left, GenericOperationTarget right) {
        this.left = left;
        this.right = right;
    }

    public static Add add_a_r8(ByteRegister right) {
        return new Add(OperationTarget.A.direct(), right.convert());
    }

    public static Add add_a_imm8() {
        return new Add(OperationTarget.A.direct(), OperationTarget.IMM_8.direct());
    }

    public static Add add_hl_r16(WordGeneralRegister right) {
        return new Add(OperationTarget.HL.direct(), right.convert());
    }

    public static Add add_sp_e8() {
        return new Add(OperationTarget.SP.direct(), OperationTarget.IMM_8.direct());
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        if (!this.left.isByteTarget() && !this.right.isByteTarget()) {
            // 16-bit addition
            executeSixteenBitAddition(cpuStructure);
        } else if (!this.left.isByteTarget()) { // && this.right.isByteTarget()
            // 16-bit + 8-bit signed
            executeSignedAddition(cpuStructure);
        } else {
            executeEightBitAddition(cpuStructure);
        }
    }

    private void executeEightBitAddition(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        byte leftValue = (byte) operationTargetAccessor.getValue(this.left);
        byte rightValue = (byte) operationTargetAccessor.getValue(this.right);

        ArithmeticResult result = cpuStructure.alu().add(leftValue, rightValue);
        operationTargetAccessor.setValue(this.left, result.result());

        result.flagChanges().forEach(
                (flag, change) -> cpuStructure.registers().setFlags(change, flag)
        );
    }

    private void executeSignedAddition(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        short leftValue = operationTargetAccessor.getValue(this.left);
        byte rightValue = (byte) operationTargetAccessor.getValue(this.right);

        short res = ControlFlow.signedAdditionOnlyAlu(leftValue, rightValue, cpuStructure);
        operationTargetAccessor.setValue(this.left, res);
    }

    private void executeSixteenBitAddition(CpuStructure cpuStructure) {
        OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
        short a = operationTargetAccessor.getValue(this.left);
        short b = operationTargetAccessor.getValue(this.right);

        byte a_lsb = lower_byte(a);
        byte b_lsb = lower_byte(b);
        ArithmeticResult lowerRes = cpuStructure.alu().add(a_lsb, b_lsb);
        short result = set_lower_byte(a, lowerRes.result());
        
        boolean carryFromLower = lowerRes.flagChanges().getOrDefault(Flag.C, false);
        
        operationTargetAccessor.setValue(this.left, result);

        cpuStructure.clock().tick();

        byte a_msb = upper_byte(a);
        byte b_msb = upper_byte(b);
        ArithmeticResult upperRes = cpuStructure.alu().add_carry(a_msb, b_msb, carryFromLower);
        result = set_upper_byte(result, upperRes.result());
        Hashtable<Flag, Boolean> flagChanges = new FlagChangesetBuilder(upperRes.flagChanges())
                .without(Flag.Z)
                .build();
        cpuStructure.registers().setFlags(flagChanges);
        operationTargetAccessor.setValue(this.left, result);
    }

    @Override
    public String representation() {
        return "ADD " + this.left.representation() + "," + this.right.representation();
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
