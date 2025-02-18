package org.gameboy;

import org.gameboy.components.CpuStructure;
import org.gameboy.instructions.Instruction;

import java.util.Objects;
import java.util.function.Consumer;

public final class TestInstruction implements Instruction {
    private final String representation;
    private Consumer<CpuStructure> behaviour;

    public TestInstruction(String representation) {
        this.representation = representation;
        this.behaviour = cpuStructure -> {};
    }

    public TestInstruction withBehaviour(Consumer<CpuStructure> behaviour) {
        this.behaviour = behaviour;
        return this;
    }

    @Override
    public void execute(CpuStructure cpuStructure) {
        behaviour.accept(cpuStructure);
    }

    @Override
    public String representation() {
        return representation;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TestInstruction) obj;
        return Objects.equals(this.representation, that.representation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(representation);
    }

    @Override
    public String toString() {
        return representation();
    }

}
