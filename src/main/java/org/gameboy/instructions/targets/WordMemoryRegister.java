package org.gameboy.instructions.targets;

public enum WordMemoryRegister {
    BC,
    DE,
    HL_INC,
    HL_DEC;

    public GenericOperationTarget convert() {
        return switch(this) {
            case BC -> GenericOperationTarget.BC;
            case DE -> GenericOperationTarget.DE;
            case HL_INC -> GenericOperationTarget.HL_INC;
            case HL_DEC -> GenericOperationTarget.HL_DEC;
        };
    }
}
