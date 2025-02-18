package org.gameboy.cpu.instructions.targets;

public record GenericOperationTarget(OperationTarget target, boolean isIndirect) {
    public String representation() {
        return isIndirect
                ? "(" + target.representation() + ")"
                : target.representation();
    }

    public boolean isByteTarget() {
        return this.isIndirect || OperationTarget.BYTE_TARGETS.contains(target);
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
