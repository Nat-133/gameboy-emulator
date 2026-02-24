package org.gameboy.cpu.instructions.common;

import org.gameboy.common.Memory;
import org.gameboy.cpu.components.CpuRegisters;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.components.IncrementDecrementUnit;
import org.gameboy.cpu.instructions.targets.Target;

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

    public short getValue(Target.R8 target) { return getValue((Target) target); }
    public short getValue(Target.Stk16 target) { return getValue((Target) target); }
    public short getValue(Target.Mem16 target) { return getValue((Target) target); }
    public short getValue(Target.R16 target) { return getValue((Target) target); }

    public short getValue(Target target) {
        return switch (target) {
            // Byte registers
            case Target.A a -> (short) uint(registers.A());
            case Target.B b -> (short) uint(registers.B());
            case Target.C c -> (short) uint(registers.C());
            case Target.D d -> (short) uint(registers.D());
            case Target.E e -> (short) uint(registers.E());
            case Target.H h -> (short) uint(registers.H());
            case Target.L l -> (short) uint(registers.L());
            // Word registers
            case Target.AF af -> registers.AF();
            case Target.BC bc -> registers.BC();
            case Target.DE de -> registers.DE();
            case Target.HL hl -> registers.HL();
            case Target.SP sp -> registers.SP();
            case Target.PC pc -> registers.PC();
            // Indirect through register
            case Target.IndirectHL ih -> readMemByte(registers.HL());
            case Target.IndirectBC ibc -> readMemByte(registers.BC());
            case Target.IndirectDE ide -> readMemByte(registers.DE());
            case Target.IndirectHLInc ihi -> {
                short addr = registers.HL();
                registers.setHL(idu.increment(addr));
                yield readMemByte(addr);
            }
            case Target.IndirectHLDec ihd -> {
                short addr = registers.HL();
                registers.setHL(idu.decrement(addr));
                yield readMemByte(addr);
            }
            // Indirect high memory (0xFF00 + offset)
            case Target.IndirectC ic -> readMemByte(set_upper_byte((short) uint(registers.C()), (byte) 0xFF));
            case Target.IndirectImm8 ii8 -> readMemByte(set_upper_byte(ControlFlow.readIndirectPCAndIncrement(cpuStructure), (byte) 0xFF));
            // Indirect through immediate address
            case Target.IndirectImm16 ii16 -> readMemByte(ControlFlow.readImm16(cpuStructure));
            // Immediates
            case Target.Imm8 i8 -> ControlFlow.readIndirectPCAndIncrement(cpuStructure);
            case Target.Imm16 i16 -> ControlFlow.readImm16(cpuStructure);
            case Target.SPOffset spo -> ControlFlow.signedAdditionWithIdu(
                    registers.SP(),
                    (byte) ControlFlow.readIndirectPCAndIncrement(cpuStructure),
                    true,
                    cpuStructure);
        };
    }

    public void setValue(Target.R8 target, short value) { setValue((Target) target, value); }
    public void setValue(Target.Stk16 target, short value) { setValue((Target) target, value); }
    public void setValue(Target.Mem16 target, short value) { setValue((Target) target, value); }
    public void setValue(Target.R16 target, short value) { setValue((Target) target, value); }

    public void setValue(Target target, short value) {
        byte byteValue = (byte) value;
        switch (target) {
            case Target.A a -> registers.setA(byteValue);
            case Target.B b -> registers.setB(byteValue);
            case Target.C c -> registers.setC(byteValue);
            case Target.D d -> registers.setD(byteValue);
            case Target.E e -> registers.setE(byteValue);
            case Target.H h -> registers.setH(byteValue);
            case Target.L l -> registers.setL(byteValue);
            case Target.AF af -> registers.setAF(value);
            case Target.BC bc -> registers.setBC(value);
            case Target.DE de -> registers.setDE(value);
            case Target.HL hl -> registers.setHL(value);
            case Target.SP sp -> registers.setSP(value);
            case Target.PC pc -> registers.setPC(value);
            case Target.IndirectHL ih -> writeMemByte(registers.HL(), byteValue);
            case Target.IndirectBC ibc -> writeMemByte(registers.BC(), byteValue);
            case Target.IndirectDE ide -> writeMemByte(registers.DE(), byteValue);
            case Target.IndirectHLInc ihi -> {
                short addr = registers.HL();
                registers.setHL(idu.increment(addr));
                writeMemByte(addr, byteValue);
            }
            case Target.IndirectHLDec ihd -> {
                short addr = registers.HL();
                registers.setHL(idu.decrement(addr));
                writeMemByte(addr, byteValue);
            }
            case Target.IndirectC ic -> writeMemByte(set_upper_byte((short) uint(registers.C()), (byte) 0xFF), byteValue);
            case Target.IndirectImm8 ii8 -> writeMemByte(set_upper_byte(ControlFlow.readIndirectPCAndIncrement(cpuStructure), (byte) 0xFF), byteValue);
            case Target.IndirectImm16 ii16 -> writeMemByte(ControlFlow.readImm16(cpuStructure), byteValue);
            case Target.Imm8 i8 -> { /* not writable */ }
            case Target.Imm16 i16 -> { /* not writable */ }
            case Target.SPOffset spo -> { /* not writable */ }
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
