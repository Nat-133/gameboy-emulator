package org.gameboy.components;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.instructions.targets.Interrupt;
import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.MemoryMapConstants.IF_ADDRESS;
import static org.gameboy.instructions.targets.Interrupt.INTERRUPT_PRIORITY;

class InterruptBusTest {
    @Test
    void givenAllInterruptsAndNoInterruptEnabled_whenGetActiveInterrupts_thenNoInterrupts() {
        Memory memory = new BasicMemory();
        InterruptBus interruptBus = new InterruptBus(memory);

        memory.write(IE_ADDRESS, (byte) 0x00);
        memory.write(IF_ADDRESS, (byte) 0xff);

        assertThat(interruptBus.activeInterrupts()).isEmpty();
    }

    @Test
    void givenAllInterruptsAndAllInterruptsEnabled_whenGetActiveInterrupts_thenAllInterrupts() {
        Memory memory = new BasicMemory();
        InterruptBus interruptBus = new InterruptBus(memory);

        memory.write(IE_ADDRESS, (byte) 0xff);
        memory.write(IF_ADDRESS, (byte) 0xff);

        assertThat(interruptBus.activeInterrupts()).containsExactlyElementsOf(INTERRUPT_PRIORITY);
    }

    @ParameterizedTest
    @EnumSource(Interrupt.class)
    void givenInterrupt_whenDeactivate_thenIFValueCorrect(Interrupt interrupt) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0xff)
                .build();

        cpuStructure.interruptBus().deactivateInterrupt(interrupt);

        byte expectedValue = BitUtilities.set_bit((byte) 0xff, interrupt.index(), false);
        assertThatHex(cpuStructure.memory().read(IF_ADDRESS)).isEqualTo(expectedValue);
    }

    @ParameterizedTest
    @ValueSource(ints={0xff, 0xab, 0x11, 0x22, 0x42, 0x01})
    void givenInterrupt_whenQueryHasInterrupt_thenCorrect(int ifValue) {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, ifValue)
                .withMemory(IE_ADDRESS, 0xfe)
                .build();

        InterruptBus interruptBus = cpuStructure.interruptBus();
        assertThat(interruptBus.hasInterrupts()).isEqualTo(!interruptBus.activeInterrupts().isEmpty());
    }
}