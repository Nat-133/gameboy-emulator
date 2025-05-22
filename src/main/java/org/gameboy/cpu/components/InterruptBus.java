package org.gameboy.cpu.components;

import org.gameboy.common.Interrupt;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryListener;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.gameboy.cpu.MemoryMapConstants.IE_ADDRESS;
import static org.gameboy.cpu.MemoryMapConstants.IF_ADDRESS;
import static org.gameboy.utils.BitUtilities.*;

public class InterruptBus {
    private static final List<Interrupt> INTERRUPT_PRIORITY = List.of(
            Interrupt.JOYPAD,
            Interrupt.SERIAL,
            Interrupt.TIMER,
            Interrupt.STAT,
            Interrupt.VBLANK
    );

    private final Memory memory;
    private final Object interruptByteLock;
    private byte currentActiveInterruptByte;
    private final Queue<CompletableFuture<Void>> interruptWaiters;

    public InterruptBus(Memory memory) {
        this.memory = memory;
        this.currentActiveInterruptByte = calculateActiveInterruptByte();
        this.interruptByteLock = new Object();
        interruptWaiters = new ArrayDeque<>();

        MemoryListener memoryListener = () -> {
            synchronized (interruptByteLock) {
                byte newInterruptByte = calculateActiveInterruptByte();
                if (newInterruptByte != 0) {
                    // potential race condition in between these lines.
                    // It is an issue if memory setting is done on multiple threads
                    currentActiveInterruptByte = newInterruptByte;

                    while (!interruptWaiters.isEmpty()) {
                        interruptWaiters.poll().complete(null);
                    }
                }
            }
        };
        memory.registerMemoryListener(IE_ADDRESS, memoryListener);
        memory.registerMemoryListener(IF_ADDRESS, memoryListener);
    }

    public boolean hasInterrupts() {
        return 0 != calculateActiveInterruptByte();
    }

    private byte calculateActiveInterruptByte() {
        return and(memory.read(IF_ADDRESS), memory.read(IE_ADDRESS));
    }

    public List<Interrupt> activeInterrupts() {
        byte interruptByte = calculateActiveInterruptByte();
        return INTERRUPT_PRIORITY.stream()
                .filter(interrupt -> get_bit(interruptByte, interrupt.index()))
                .toList();
    }

    public void deactivateInterrupt(Interrupt interrupt) {
        byte newInterruptFlag = set_bit(memory.read(IF_ADDRESS), interrupt.index(), false);
        memory.write(IF_ADDRESS, newInterruptFlag);
    }

    public void waitForInterrupt() {
        CompletableFuture<Void> interruptMemoryChange = new CompletableFuture<>();

        synchronized (interruptByteLock) {
            if (currentActiveInterruptByte != 0) {
                return;
            }

            interruptWaiters.add(interruptMemoryChange);
        }

        try {
            interruptMemoryChange.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
