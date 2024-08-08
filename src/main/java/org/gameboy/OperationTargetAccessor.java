package org.gameboy;

import org.gameboy.instructions.targets.GenericOperationTarget;
import org.gameboy.instructions.targets.OperationTarget;

import static org.gameboy.utils.BitUtilities.*;

public class OperationTargetAccessor {
    Memory memory;
    CpuRegisters cpuRegisters;

    public OperationTargetAccessor(Memory memory, CpuRegisters cpuRegisters) {
        this.memory = memory;
        this.cpuRegisters = cpuRegisters;
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
            case B -> (short) uint(cpuRegisters.B());
            case C -> (short) uint(cpuRegisters.C());
            case D -> (short) uint(cpuRegisters.D());
            case E -> (short) uint(cpuRegisters.E());
            case H -> (short) uint(cpuRegisters.H());
            case L -> (short) uint(cpuRegisters.L());
            case A -> (short) uint(cpuRegisters.A());
            case AF -> cpuRegisters.AF();
            case BC -> cpuRegisters.BC();
            case DE -> cpuRegisters.DE();
            case HL -> cpuRegisters.HL();
            case SP -> cpuRegisters.SP();
            case HL_INC -> {
                short val = cpuRegisters.HL();
                cpuRegisters.setHL((short)(val + 1));
                yield val;
            }
            case HL_DEC -> {
                short val = cpuRegisters.HL();
                cpuRegisters.setHL((short)(val - 1));
                yield val;
            }
            case IMM_8 -> upper_byte(cpuRegisters.instructionRegister());
            case IMM_16 -> memory.read((short)(cpuRegisters.PC()+1));  // todo: unchecked
        };
    }

    private short getIndirectValue(OperationTarget target) {
        short directValue = getDirectValue(target);
        return memory.read(directValue);
    }

    private void setDirectValue(OperationTarget target, short value) {
        byte byteValue = (byte) value;
        switch(target) {
            case A -> cpuRegisters.setA(byteValue);
            case B -> cpuRegisters.setB(byteValue);
            case C -> cpuRegisters.setC(byteValue);
            case D -> cpuRegisters.setD(byteValue);
            case E -> cpuRegisters.setE(byteValue);
            case H -> cpuRegisters.setH(byteValue);
            case L -> cpuRegisters.setL(byteValue);
            case AF -> cpuRegisters.setAF(value);
            case BC -> cpuRegisters.setBC(value);
            case DE -> cpuRegisters.setDE(value);
            case HL -> cpuRegisters.setHL(value);
            case SP -> cpuRegisters.setSP(value);
            case HL_INC -> {
                short val = cpuRegisters.HL();
                cpuRegisters.setHL((short)(val + 1));
                memory.write(val, byteValue);
            }
            case HL_DEC -> {
                short val = cpuRegisters.HL();
                cpuRegisters.setHL((short)(val - 1));
                memory.write(val, byteValue);
            }
            case IMM_8, IMM_16 -> {
                // not needed
            }
        }
    }

    private void setIndirectValue(OperationTarget target, short value) {
        short directValue = getDirectValue(target);
        memory.write(directValue, (byte) value);
    }
}
