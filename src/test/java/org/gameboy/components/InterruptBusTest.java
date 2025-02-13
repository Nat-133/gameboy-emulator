package org.gameboy.components;

import org.gameboy.CpuStructureBuilder;
import org.gameboy.instructions.targets.Interrupt;
import org.gameboy.utils.BitUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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

    @Test
    void givenNoInterrupt_whenWaitForInterrupt_andInterruptSent_thenWaitFinishes() throws TimeoutException {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0)
                .withMemory(IE_ADDRESS, 0xff)
                .build();
        InterruptBus interruptBus = cpuStructure.interruptBus();
        Memory memory = cpuStructure.memory();

        Thread waitThread = new Thread(interruptBus::waitForInterrupt);
        Thread writeThread = new Thread(() -> memory.write(IF_ADDRESS, (byte) 0x0f));

        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);

        writeThread.start();
        waitFor(() -> !writeThread.isAlive());

        waitFor(() -> !waitThread.isAlive());
        assertThat(waitThread.isAlive()).isFalse();
    }

    @Test
    void givenInterruptsRequestedButDisabled_whenWaitForInterrupt_andInterruptsEnabled_thenWaitFinishes() throws TimeoutException {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0xff)
                .withMemory(IE_ADDRESS, 0x00)
                .build();
        InterruptBus interruptBus = cpuStructure.interruptBus();
        Memory memory = cpuStructure.memory();


        Thread waitThread = new Thread(interruptBus::waitForInterrupt);
        Thread writeThread = new Thread(() -> memory.write(IE_ADDRESS, (byte) 0xff));

        waitThread.start();
        waitFor(() -> waitThread.getState() == Thread.State.WAITING || waitThread.getState() == Thread.State.BLOCKED);

        writeThread.start();
        waitFor(() -> !writeThread.isAlive());

        waitFor(() -> !waitThread.isAlive());
        assertThat(waitThread.isAlive()).isFalse();
    }

    @Test
    void givenNoInterrupt_whenWaitForInterrupt_andNoInterruptSent_thenWaitDoesNotFinish() throws ExecutionException, InterruptedException {
        CpuStructure cpuStructure = new CpuStructureBuilder()
                .withMemory(IF_ADDRESS, 0x00)
                .withMemory(IE_ADDRESS, 0x00)
                .build();
        InterruptBus interruptBus = cpuStructure.interruptBus();

        CompletableFuture<Void> interruptWait = CompletableFuture.runAsync(interruptBus::waitForInterrupt);

        try {
            interruptWait.get(100, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return;
        }
        fail("waitForInterrupt returned without interrupt.");
    }

    static void waitFor(Supplier<Boolean> condition) throws TimeoutException {
        Instant end = Instant.now().plusMillis(100);
        while (!condition.get()) {
            if (Instant.now().isAfter(end)) {
                throw new TimeoutException("Timed out waiting for condition.");
            }
            Thread.yield();
        }
    }
}