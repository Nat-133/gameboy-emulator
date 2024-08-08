package org.gameboy.instructions.targets;

public record GenericOperationTarget(OperationTarget target, boolean isIndirect) {
    public String representation() {
        return isIndirect
                ? "(" + target.representation() + ")"
                : target.representation();
    }
}
