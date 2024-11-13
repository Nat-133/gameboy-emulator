package org.gameboy.instructions.targets;

import org.gameboy.utils.MultiBitValue.ThreeBitValue;

public enum ByteRegister {
    // r8
    B,
    C,
    D,
    E,
    H,
    L,
    INDIRECT_HL,
    A;

    public GenericOperationTarget convert(){
        return switch(this){
            case B -> OperationTarget.B.direct();
            case C -> OperationTarget.C.direct();
            case D -> OperationTarget.D.direct();
            case E -> OperationTarget.E.direct();
            case H -> OperationTarget.H.direct();
            case L -> OperationTarget.L.direct();
            case INDIRECT_HL -> OperationTarget.HL.indirect();
            case A -> OperationTarget.A.direct();
        };
    }

    public static ByteRegister lookup(ThreeBitValue index) {
        return ByteRegister.values()[index.value()];
    }
}
