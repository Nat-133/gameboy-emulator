package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import static org.gameboy.common.MemoryMapConstants.OAM_SIZE;
import static org.gameboy.common.MemoryMapConstants.OAM_START_ADDRESS;

@Singleton
public class MemoryBus implements Memory, DmaController {
    private final Memory underlying;
    private boolean listenerRegistered = false;

    private enum DmaPhase {
        INACTIVE,
        REQUESTED,    // Same cycle as write to $FF46
        PENDING,      // Delay before transfers begin
        TRANSFERRING
    }

    // DMA state machine
    private DmaPhase dmaPhase;
    private boolean blockingDuringSetup;  // True when DMA restarts during active transfer

    // DMA transfer state - what/where we're copying
    private short dmaSourceAddress;
    private int dmaByteIndex;

    @Inject
    public MemoryBus(@Named("underlying") Memory underlying) {
        this.underlying = underlying;

        this.dmaPhase = DmaPhase.INACTIVE;
        this.blockingDuringSetup = false;

        this.dmaSourceAddress = 0;
        this.dmaByteIndex = 0;
    }

    private void ensureDmaListenerRegistered() {
        if (!listenerRegistered) {
            underlying.registerMemoryListener(MemoryMapConstants.DMA_REGISTER_ADDRESS, () -> {
                byte sourceHigh = underlying.read(MemoryMapConstants.DMA_REGISTER_ADDRESS);
                startDma(sourceHigh);
            });
            listenerRegistered = true;
        }
    }

    @Override
    public void startDma(byte sourceHigh) {
        // When restarting during active transfer, memory remains blocked during setup
        blockingDuringSetup = (dmaPhase == DmaPhase.TRANSFERRING);
        dmaPhase = DmaPhase.REQUESTED;
        dmaSourceAddress = (short) ((sourceHigh & 0xFF) << 8);
        dmaByteIndex = 0;
    }

    @Override
    public byte read(short address) {
        ensureDmaListenerRegistered();
        if (isBlocking() && !isAccessibleDuringDma(address)) {
            return (byte) 0xFF;
        }
        return underlying.read(address);
    }

    private boolean isBlocking() {
        return switch (dmaPhase) {
            case INACTIVE -> false;
            case REQUESTED, PENDING -> blockingDuringSetup;
            case TRANSFERRING -> true;
        };
    }

    @Override
    public void write(short address, byte value) {
        ensureDmaListenerRegistered();
        underlying.write(address, value);
    }

    @Override
    public void registerMemoryListener(short address, MemoryListener listener) {
        underlying.registerMemoryListener(address, listener);
    }

    @Override
    public void mCycle() {
        switch (dmaPhase) {
            case INACTIVE -> {}
            case REQUESTED -> dmaPhase = DmaPhase.PENDING;
            case PENDING -> dmaPhase = DmaPhase.TRANSFERRING;
            case TRANSFERRING -> {
                short sourceAddr = (short) (dmaSourceAddress + dmaByteIndex);
                short destAddr = (short) (OAM_START_ADDRESS + dmaByteIndex);
                byte data = underlying.read(sourceAddr);
                underlying.write(destAddr, data);
                dmaByteIndex++;
                if (dmaByteIndex >= OAM_SIZE) {
                    dmaPhase = DmaPhase.INACTIVE;
                }
            }
        }
    }

    @Override
    public boolean isDmaActive() {
        return dmaPhase != DmaPhase.INACTIVE;
    }

    private boolean isAccessibleDuringDma(short address) {
        int addr = address & 0xFFFF;
        // During DMA, allow access to HRAM (0xFF80-0xFFFE) and I/O registers (0xFF00-0xFF7F)
        return (addr >= 0xFF00 && addr <= 0xFFFE);
    }
}
