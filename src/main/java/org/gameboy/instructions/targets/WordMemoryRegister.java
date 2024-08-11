package org.gameboy.instructions.targets;

public enum WordMemoryRegister {
    // mem16
    BC,
    DE,
    HL_INC,
    HL_DEC;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> OperationTarget.BC.indirect();
            case DE -> OperationTarget.DE.indirect();
            case HL_INC -> OperationTarget.HL_INC.indirect();
            case HL_DEC -> OperationTarget.HL_DEC.indirect();
        };
    }
}
