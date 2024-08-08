package org.gameboy.instructions.targets;

public enum WordGeneralRegister {
    BC,
    DE,
    HL,
    SP;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> OperationTarget.BC.direct();
            case DE -> OperationTarget.DE.direct();
            case HL -> OperationTarget.HL.direct();
            case SP -> OperationTarget.SP.direct();
        };
    }
}
