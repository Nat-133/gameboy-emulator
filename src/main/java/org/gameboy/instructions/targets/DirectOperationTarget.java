package org.gameboy.instructions.targets;

public enum DirectOperationTarget {
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    H("H"),
    L("L"),
    A("A"),
    AF("AF"),
    BC("BC"),
    DE("DE"),
    HL("HL"),
    SP("SP"),
    HL_INC("HL+"),
    HL_DEC("HL-"),
    HL_INDIRECT("(HL)"),
    IMM_8("imm8"),
    IMM_16("imm16"),
    IMM_8_INDIRECT("(imm8)"),
    IMM_16_INDIRECT("(imm16)");

    private final String representation;

    DirectOperationTarget(String representation) {
        this.representation = representation;
    }

    public String representation() {
        return representation;
    }
}
