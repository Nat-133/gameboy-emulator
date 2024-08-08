package org.gameboy.instructions.targets;

public record GenericOperationTarget(OperationTarget target, boolean isIndirect) {
    public String representation() {
        return isIndirect
                ? "(" + target.representation() + ")"
                : target.representation();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GenericOperationTarget that) {
            return this.target == that.target && this.isIndirect == that.isIndirect;
        }
        return false;

    }

    @Override
    public String toString() {
        return this.representation();
    }
}
