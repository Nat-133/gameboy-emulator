package org.gameboy.instructions.targets;

public enum WordStackRegister {
    BC,
    DE,
    HL,
    AF;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> GenericOperationTarget.BC;
            case DE -> GenericOperationTarget.DE;
            case HL -> GenericOperationTarget.HL;
            case AF -> GenericOperationTarget.AF;
        };
    }
}
