package org.gameboy.cpu.instructions.common;

import org.gameboy.common.Memory;
import org.gameboy.cpu.components.CpuRegisters;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.components.IncrementDecrementUnit;
import org.gameboy.cpu.instructions.targets.Target;
import static org.gameboy.cpu.instructions.targets.Target.*;

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

    public short getValue(Target target) {
        return switch (target) {
            // Byte registers
            case A a -> (short) uint(registers.A());
            case B b -> (short) uint(registers.B());
            case C c -> (short) uint(registers.C());
            case D d -> (short) uint(registers.D());
            case E e -> (short) uint(registers.E());
            case H h -> (short) uint(registers.H());
            case L l -> (short) uint(registers.L());
            // Word registers
            case AF af -> registers.AF();
            case BC bc -> registers.BC();
            case DE de -> registers.DE();
            case HL hl -> registers.HL();
            case SP sp -> registers.SP();
            case PC pc -> registers.PC();
            // Indirect through register
            case IndirectHL ih -> readMemByte(registers.HL());
            case IndirectBC ibc -> readMemByte(registers.BC());
            case IndirectDE ide -> readMemByte(registers.DE());
            case IndirectHLInc ihi -> {
                short addr = registers.HL();
                registers.setHL(idu.increment(addr));
                yield readMemByte(addr);
            }
            case IndirectHLDec ihd -> {
                short addr = registers.HL();
                registers.setHL(idu.decrement(addr));
                yield readMemByte(addr);
            }
            // Indirect high memory (0xFF00 + offset)
            case IndirectC ic -> readMemByte(set_upper_byte((short) uint(registers.C()), (byte) 0xFF));
            case IndirectImm8 ii8 -> readMemByte(set_upper_byte(ControlFlow.readIndirectPCAndIncrement(cpuStructure), (byte) 0xFF));
            // Indirect through immediate address
            case IndirectImm16 ii16 -> readMemByte(ControlFlow.readImm16(cpuStructure));
            // Immediates
            case Imm8 i8 -> ControlFlow.readIndirectPCAndIncrement(cpuStructure);
            case Imm16 i16 -> ControlFlow.readImm16(cpuStructure);
            case SPOffset spo -> ControlFlow.signedAdditionWithIdu(
                    registers.SP(),
                    (byte) ControlFlow.readIndirectPCAndIncrement(cpuStructure),
                    true,
                    cpuStructure);
        };
    }

    public void setValue(Target target, short value) {
        byte byteValue = (byte) value;
        switch (target) {
            case A a -> registers.setA(byteValue);
            case B b -> registers.setB(byteValue);
            case C c -> registers.setC(byteValue);
            case D d -> registers.setD(byteValue);
            case E e -> registers.setE(byteValue);
            case H h -> registers.setH(byteValue);
            case L l -> registers.setL(byteValue);
            case AF af -> registers.setAF(value);
            case BC bc -> registers.setBC(value);
            case DE de -> registers.setDE(value);
            case HL hl -> registers.setHL(value);
            case SP sp -> registers.setSP(value);
            case PC pc -> registers.setPC(value);
            case IndirectHL ih -> writeMemByte(registers.HL(), byteValue);
            case IndirectBC ibc -> writeMemByte(registers.BC(), byteValue);
            case IndirectDE ide -> writeMemByte(registers.DE(), byteValue);
            case IndirectHLInc ihi -> {
                short addr = registers.HL();
                registers.setHL(idu.increment(addr));
                writeMemByte(addr, byteValue);
            }
            case IndirectHLDec ihd -> {
                short addr = registers.HL();
                registers.setHL(idu.decrement(addr));
                writeMemByte(addr, byteValue);
            }
            case IndirectC ic -> writeMemByte(set_upper_byte((short) uint(registers.C()), (byte) 0xFF), byteValue);
            case IndirectImm8 ii8 -> writeMemByte(set_upper_byte(ControlFlow.readIndirectPCAndIncrement(cpuStructure), (byte) 0xFF), byteValue);
            case IndirectImm16 ii16 -> writeMemByte(ControlFlow.readImm16(cpuStructure), byteValue);
            case Imm8 i8 -> { /* not writable */ }
            case Imm16 i16 -> { /* not writable */ }
            case SPOffset spo -> { /* not writable */ }
        }
    }

    private short readMemByte(short address) {
        byte read = memory.read(address);
        cpuStructure.clock().tick();
        return read;
    }

    private void writeMemByte(short address, byte value) {
        memory.write(address, value);
        cpuStructure.clock().tick();
    }
}
