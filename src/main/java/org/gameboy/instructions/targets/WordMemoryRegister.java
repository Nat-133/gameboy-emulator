package org.gameboy.instructions.targets;

public enum WordMemoryRegister {
    BC,
    DE,
    HL_INC,
    HL_DEC;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> OperationTarget.BC.direct();
            case DE -> OperationTarget.DE.direct();
            case HL_INC -> OperationTarget.HL_INC.indirect();
            case HL_DEC -> OperationTarget.HL_DEC.indirect();
        };
    }
}
