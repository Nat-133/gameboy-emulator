package org.gameboy.instructions.targets;

import org.gameboy.utils.MultiBitValue;

public enum WordStackRegister {
    // stk16
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

    public static WordStackRegister lookup(MultiBitValue.TwoBitValue index) {
        return WordStackRegister.values()[index.value()];
    }
}
