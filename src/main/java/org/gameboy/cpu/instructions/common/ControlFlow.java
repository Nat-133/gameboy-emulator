package org.gameboy.cpu.instructions.common;

import org.gameboy.cpu.ArithmeticResult;
import org.gameboy.cpu.Flag;
import org.gameboy.cpu.FlagChangesetBuilder;
import org.gameboy.cpu.components.CpuRegisters;
import org.gameboy.cpu.components.CpuStructure;
import org.gameboy.cpu.instructions.targets.Condition;

import java.util.Hashtable;

import static org.gameboy.utils.BitUtilities.*;

public class ControlFlow {
    public static short signedAdditionOnlyAlu(short a, byte signedByte, CpuStructure cpuStructure) {
        byte msb = upper_byte(a);
        byte lsb = lower_byte(a);

        ArithmeticResult res = cpuStructure.alu().add(lsb, signedByte);
        Hashtable<Flag, Boolean> flagChanges = new FlagChangesetBuilder(res.flagChanges())
                .with(Flag.Z, false)
                .with(Flag.N, false)
                .build();
        flagChanges.forEach((f,b) -> cpuStructure.registers().setFlags(b, f));
        lsb = res.result();

        boolean carry = res.flagChanges().getOrDefault(Flag.C, false);
        boolean negativeOffset = get_bit(signedByte, 7);

        cpuStructure.clock().tick();

        if (carry && !negativeOffset) {
            msb = cpuStructure.alu().inc(msb).result();
        }
        else if (!carry && negativeOffset) {
            msb = cpuStructure.alu().dec(msb).result();
        }

        cpuStructure.clock().tick();

        return concat(msb, lsb);
    }

    public static short signedAdditionWithIdu(short a, byte signedByte, boolean setFlags, CpuStructure cpuStructure) {
        byte msb = upper_byte(a);
        byte lsb = lower_byte(a);

        ArithmeticResult res = cpuStructure.alu().add(lsb, signedByte);
        if (setFlags) res.flagChanges().forEach((f,b) -> cpuStructure.registers().setFlags(b, f));
        lsb = res.result();

        boolean carry = res.flagChanges().getOrDefault(Flag.C, false);
        boolean negativeOffset = get_bit(signedByte, 7);

        if (carry && !negativeOffset) {
            msb = (byte) cpuStructure.idu().increment(msb);
        }
        else if (!carry && negativeOffset) {
            msb = (byte) cpuStructure.idu().decrement(msb);
        }

        cpuStructure.clock().tick();

        return concat(msb, lsb);
    }

    public static void incrementPC(CpuStructure cpuStructure) {
        cpuStructure.registers().setPC(cpuStructure.idu().increment(cpuStructure.registers().PC()));
    }

    public static byte readIndirectPCAndIncrement(CpuStructure cpuStructure) {
        byte value = cpuStructure.memory().read(cpuStructure.registers().PC());
        incrementPC(cpuStructure);
        cpuStructure.clock().tick();
        return value;
    }

    public static short readImm16(CpuStructure cpuStructure) {
        byte lsb = readIndirectPCAndIncrement(cpuStructure);

        byte msb = readIndirectPCAndIncrement(cpuStructure);

        return concat(msb, lsb);
    }

    public static void writeWordToMem(short address, short val, CpuStructure cpuStructure) {
        byte msb = upper_byte(val);
        byte lsb = lower_byte(val);

        cpuStructure.memory().write(address, lsb);
        short nextAddress = cpuStructure.idu().increment(address);
        cpuStructure.clock().tick();

        cpuStructure.memory().write(nextAddress, msb);

        cpuStructure.clock().tick();
    }

    public static void decrementSP(CpuStructure cpuStructure) {
        cpuStructure.registers().setSP(cpuStructure.idu().decrement(cpuStructure.registers().SP()));
    }

    public static void incrementSP(CpuStructure cpuStructure) {
        cpuStructure.registers().setSP(cpuStructure.idu().increment(cpuStructure.registers().SP()));
    }

    public static void pushToStack(CpuStructure cpuStructure, short value) {
        decrementSP(cpuStructure);

        cpuStructure.clock().tick();

        cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));
        decrementSP(cpuStructure);

        cpuStructure.clock().tick();

        cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));

        cpuStructure.clock().tick();
    }

    public static short popFromStack(CpuStructure cpuStructure) {
        byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
        incrementSP(cpuStructure);

        cpuStructure.clock().tick();

        byte msb = cpuStructure.memory().read(cpuStructure.registers().SP());
        incrementSP(cpuStructure);

        cpuStructure.clock().tick();

        return concat(msb, lsb);
    }

    public static boolean evaluateCondition(Condition condition, CpuRegisters registers) {
        return switch(condition) {
            case null -> true;
            case NZ -> !registers.getFlag(Flag.Z);
            case Z -> registers.getFlag(Flag.Z);
            case NC -> !registers.getFlag(Flag.C);
            case C -> registers.getFlag(Flag.C);
        };
    }
}
