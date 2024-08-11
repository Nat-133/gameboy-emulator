package org.gameboy.instructions.targets;

public enum OperationTarget {
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
    IMM_8("imm8"),
    IMM_16("imm16"),
    SP_OFFSET("SP+e8");

    private final String representation;

    OperationTarget(String representation) {
        this.representation = representation;
    }

    public String representation() {
        return representation;
    }

    public GenericOperationTarget direct() {
        return getGenericOperationTarget(false);
    }

    public GenericOperationTarget indirect() {
        return getGenericOperationTarget(true);
    }

    public GenericOperationTarget getGenericOperationTarget(boolean indirect) {
        return new GenericOperationTarget(this, indirect);
    }
}
