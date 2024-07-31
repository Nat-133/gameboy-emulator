package org.gameboy.instructions.targets;

public enum WordGeneralRegister {
    BC,
    DE,
    HL,
    SP;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> GenericOperationTarget.BC;
            case DE -> GenericOperationTarget.DE;
            case HL -> GenericOperationTarget.HL;
            case SP -> GenericOperationTarget.SP;
        };
    }
}
