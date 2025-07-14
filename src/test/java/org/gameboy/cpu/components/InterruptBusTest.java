package org.gameboy.cpu.components;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.common.BasicMemory;
import org.gameboy.common.ClockWithParallelProcess;
import org.gameboy.common.Interrupt;
import org.gameboy.common.Memory;
import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.gameboy.GameboyAssertions.assertThatHex;
import static org.gameboy.common.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.common.MemoryMapConstants.IF_ADDRESS;

class InterruptBusTest {
    private static final List<Interrupt> INTERRUPT_PRIORITY = List.of(
            Interrupt.JOYPAD,
            Interrupt.SERIAL,
            Interrupt.TIMER,
            Interrupt.STAT,
            Interrupt.VBLANK
    );

    @Test
    void givenAllInterruptsAndNoInterruptEnabled_whenGetActiveInterrupts_thenNoInterrupts() {
        Memory memory = new BasicMemory();
        InterruptBus interruptBus = new InterruptBus(memory, new ClockWithParallelProcess(() -> {}));

        memory.write(IE_ADDRESS, (byte) 0x00);
        memory.write(IF_ADDRESS, (byte) 0xff);

        assertThat(interruptBus.activeInterrupts()).isEmpty();
    }

    @Test
    void givenAllInterruptsAndAllInterruptsEnabled_whenGetActiveInterrupts_thenAllInterrupts() {
        Memory memory = new BasicMemory();
        InterruptBus interruptBus = new InterruptBus(memory, new ClockWithParallelProcess(() -> {}));

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

    @Test
    void givenNoInterrupt_whenWaitForInterrupt_andInterruptSent_thenWaitFinishes() {
        Memory memory = new BasicMemory();
        memory.write(IF_ADDRESS, (byte) 0);
        memory.write(IE_ADDRESS, (byte) 0xff);
        
        AtomicInteger tickCount = new AtomicInteger(0);
        
        ClockWithParallelProcess clock = new ClockWithParallelProcess(() -> {
            if (tickCount.incrementAndGet() == 10) {
                memory.write(IF_ADDRESS, (byte) 0x0f);
            }
        });
        
        InterruptBus interruptBus = new InterruptBus(memory, clock);
        
        interruptBus.waitForInterrupt();
        
        assertThat(tickCount.get()).isEqualTo(10);
        assertThat(interruptBus.hasInterrupts()).isTrue();
    }

    @Test
    void givenInterruptsRequestedButDisabled_whenWaitForInterrupt_andInterruptsEnabled_thenWaitFinishes() {
        Memory memory = new BasicMemory();
        memory.write(IF_ADDRESS, (byte) 0xff);
        memory.write(IE_ADDRESS, (byte) 0x00);
        
        AtomicInteger tickCount = new AtomicInteger(0);
        
        ClockWithParallelProcess clock = new ClockWithParallelProcess(() -> {
            if (tickCount.incrementAndGet() == 10) {
                memory.write(IE_ADDRESS, (byte) 0xff);
            }
        });
        
        InterruptBus interruptBus = new InterruptBus(memory, clock);
        
        interruptBus.waitForInterrupt();
        
        assertThat(tickCount.get()).isEqualTo(10);
        assertThat(interruptBus.hasInterrupts()).isTrue();
    }

    @Test
    void givenNoInterrupt_whenWaitForInterrupt_andNoInterruptSent_thenWaitDoesNotFinish() {
        Memory memory = new BasicMemory();
        memory.write(IF_ADDRESS, (byte) 0x00);
        memory.write(IE_ADDRESS, (byte) 0x00);
        
        AtomicInteger tickCount = new AtomicInteger(0);
        
        ClockWithParallelProcess clock = new ClockWithParallelProcess(() -> {
            int count = tickCount.incrementAndGet();

            if (count >= 100) {
                throw new RuntimeException("Test timeout - no interrupt received after 100 ticks");
            }
        });
        
        InterruptBus interruptBus = new InterruptBus(memory, clock);
        
        try {
            interruptBus.waitForInterrupt();
            fail("waitForInterrupt should not have completed without an interrupt");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Test timeout");
            assertThat(tickCount.get()).isEqualTo(100);
        }
    }
}