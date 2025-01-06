package org.gameboy.instructions.common;

import org.gameboy.components.CpuRegisters;
import org.gameboy.components.CpuStructure;
import org.gameboy.components.IncrementDecrementUnit;
import org.gameboy.components.Memory;
import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

import static org.gameboy.utils.BitUtilities.set_upper_byte;
import static org.gameboy.utils.BitUtilities.uint;

public class OperationTargetAccessor {
    private final Memory memory;
    private final CpuRegisters registers;
    private final IncrementDecrementUnit idu;
    private final CpuStructure cpuStructure;

    private OperationTargetAccessor(CpuStructure cpuStructure) {
        this.cpuStructure = cpuStructure;
        this.memory = cpuStructure.memory();
        this.registers = cpuStructure.registers();
        this.idu = cpuStructure.idu();
    }

    public static OperationTargetAccessor from(CpuStructure cpuStructure) {
        return new OperationTargetAccessor(cpuStructure);
    }

    public void setValue(GenericOperationTarget target, short value) {
        if (target.isIndirect()) {
            setIndirectValue(target.target(), value);
        } else {
            setDirectValue(target.target(), value);
        }
    }

    public short getValue(GenericOperationTarget target) {
        return target.isIndirect()
                ? getIndirectValue(target.target())
                : getDirectValue(target.target());
    }

    private short getDirectValue(OperationTarget target) {
        return switch(target) {
            case B -> (short) uint(registers.B());
            case C -> (short) uint(registers.C());
            case D -> (short) uint(registers.D());
            case E -> (short) uint(registers.E());
            case H -> (short) uint(registers.H());
            case L -> (short) uint(registers.L());
            case A -> (short) uint(registers.A());
            case AF -> registers.AF();
            case BC -> registers.BC();
            case DE -> registers.DE();
            case HL -> registers.HL();
            case SP -> registers.SP();
            case PC -> registers.PC();
            case HL_INC -> {
                short val = registers.HL();
                registers.setHL(idu.increment(val));
                yield val;
            }
            case HL_DEC -> {
                short val = registers.HL();
                registers.setHL(idu.decrement(val));
                yield val;
            }
            case IMM_8 -> ControlFlow.readImm8(cpuStructure);
            case IMM_16 -> ControlFlow.readImm16(cpuStructure);
            case SP_OFFSET -> ControlFlow.signedAdditionWithIdu(
                    registers.SP(),
                    (byte) this.getDirectValue(OperationTarget.IMM_8),
                    true,
                    cpuStructure);
        };
    }

    private short getIndirectValue(OperationTarget target) {
        short directValue = getDirectValue(target);
        if (target == OperationTarget.C || target == OperationTarget.IMM_8) {
            directValue = set_upper_byte(directValue, (byte) 0xFF);
        }
        byte read = memory.read(directValue);

        cpuStructure.clock().tick();

        return read;
    }

    private void setDirectValue(OperationTarget target, short value) {
        byte byteValue = (byte) value;
        switch(target) {
            case A -> registers.setA(byteValue);
            case B -> registers.setB(byteValue);
            case C -> registers.setC(byteValue);
            case D -> registers.setD(byteValue);
            case E -> registers.setE(byteValue);
            case H -> registers.setH(byteValue);
            case L -> registers.setL(byteValue);
            case AF -> registers.setAF(value);
            case BC -> registers.setBC(value);
            case DE -> registers.setDE(value);
            case HL -> registers.setHL(value);
            case SP -> registers.setSP(value);
            case PC -> registers.setPC(value);
            case HL_INC, HL_DEC, IMM_8, IMM_16, SP_OFFSET -> {
                // not needed
            }
        }
    }

    private void setIndirectValue(OperationTarget target, short value) {
        short directValue = getDirectValue(target);

        if (target == OperationTarget.C || target == OperationTarget.IMM_8) {
            directValue = set_upper_byte(directValue, (byte) 0xFF);
        }

        memory.write(directValue, (byte) value);

        cpuStructure.clock().tick();
    }
}
