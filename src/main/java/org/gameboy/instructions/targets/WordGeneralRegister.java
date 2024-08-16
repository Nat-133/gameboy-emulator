package org.gameboy.instructions.targets;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

public enum WordGeneralRegister {
    // r16
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

    public WordGeneralRegister value(TwoBitValue value) {
        return values()[value.ordinal()];
    }
}
