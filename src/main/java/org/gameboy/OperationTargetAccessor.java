package org.gameboy;

import org.gameboy.instructions.targets.GenericOperationTarget;

import static org.gameboy.utils.BitUtilities.*;

public class OperationTargetAccessor {
    Memory memory;
    CpuRegisters cpuRegisters;

    public OperationTargetAccessor(Memory memory, CpuRegisters cpuRegisters) {
        this.memory = memory;
        this.cpuRegisters = cpuRegisters;
    }

    public short getValue(GenericOperationTarget target) {
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
            case HL_INDIRECT -> memory.read(cpuRegisters.HL());
            case HL_INC -> {
                short val = cpuRegisters.HL();
                cpuRegisters.setHL((short)(val + 1));
                yield memory.read(val);
            }
            case HL_DEC -> {
                short val = cpuRegisters.HL();
                cpuRegisters.setHL((short)(val - 1));
                yield memory.read(val);
            }
            case IMM_8_INDIRECT -> {
                short address = (short)((short)0xFF00 + rshift(cpuRegisters.instructionRegister(), 8));
                yield memory.read(address);
            }
            case IMM_16_INDIRECT -> memory.read(memory.read((short)(cpuRegisters.PC()+1)));  //todo:unchecked
            case IMM_8 -> upper_byte(cpuRegisters.instructionRegister());
            case IMM_16 -> memory.read((short)(cpuRegisters.PC()+1));  // todo: unchecked
        };
    }

    public void setValue(GenericOperationTarget target, short value) {
        byte byteValue = (byte) value;
        switch(target) {
            case B -> {
                cpuRegisters.setB(byteValue);
            }
            case C -> cpuRegisters.setC(byteValue);
            case D -> cpuRegisters.setD(byteValue);
            case E -> cpuRegisters.setE(byteValue);
            case H -> cpuRegisters.setH(byteValue);
            case L -> cpuRegisters.setL(byteValue);
            case A -> cpuRegisters.setA(byteValue);
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
            case HL_INDIRECT -> memory.write(cpuRegisters.HL(), byteValue);
            case IMM_8, IMM_16 -> {
                // not needed
            }
            case IMM_8_INDIRECT -> {
                short address = set_lower_byte((short) 0xFF00, upper_byte(cpuRegisters.instructionRegister()));
                memory.write(address, byteValue);
            }
            case IMM_16_INDIRECT -> {
                short address = memory.read((short) (cpuRegisters.PC() + 1));  // todo: unchecked
                memory.write(address, byteValue);
            }
        }
    }
}
