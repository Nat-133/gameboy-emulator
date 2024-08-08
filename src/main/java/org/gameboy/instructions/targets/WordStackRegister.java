package org.gameboy.instructions.targets;

public enum WordStackRegister {
    BC,
    DE,
    HL,
    AF;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> OperationTarget.BC.direct();
            case DE -> OperationTarget.DE.direct();
            case HL -> OperationTarget.HL.direct();
            case AF -> OperationTarget.AF.direct();
        };
    }
}
