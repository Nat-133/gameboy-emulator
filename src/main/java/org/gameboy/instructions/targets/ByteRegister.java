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
            case B -> GenericOperationTarget.B;
            case C -> GenericOperationTarget.C;
            case D -> GenericOperationTarget.D;
            case E -> GenericOperationTarget.E;
            case H -> GenericOperationTarget.H;
            case L -> GenericOperationTarget.L;
            case INDIRECT_HL -> GenericOperationTarget.HL_INDIRECT;
            case A -> GenericOperationTarget.A;
        };
    }
}
