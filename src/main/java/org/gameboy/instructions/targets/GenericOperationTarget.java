package org.gameboy.instructions.targets;

public record GenericOperationTarget(DirectOperationTarget target, boolean indirect) {
    public String representation() {
        return indirect
                ? "(" + target.representation() + ")"
                : target.representation();
    }
}
