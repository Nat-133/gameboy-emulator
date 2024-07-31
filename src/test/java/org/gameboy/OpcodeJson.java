package org.gameboy;

import java.util.Map;

record OpcodeJson(
        Map<String, InstructionData> unprefixed,
        Map<String, InstructionData> prefixed
){
    public record InstructionData(
            String mnemonic,
            int bytes,
            int[] cycles,
            OperandData[] operands,
            boolean immediate,
            FlagData flags
    ) {}

    public record OperandData(
            String name,
            boolean immediate
    ){}

    public record FlagData(
            String Z,
            String N,
            String H,
            String C
    ){}
}
