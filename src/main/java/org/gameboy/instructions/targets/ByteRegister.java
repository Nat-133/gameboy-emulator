package org.gameboy.instructions.targets;

public enum ByteRegister {
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
}
