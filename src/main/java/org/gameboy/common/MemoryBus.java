package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import static org.gameboy.common.MemoryMapConstants.OAM_SIZE;
import static org.gameboy.common.MemoryMapConstants.OAM_START_ADDRESS;

@Singleton
public class MemoryBus implements Memory, DmaController {
    private final Memory underlying;
    private boolean dmaActive;
    private short dmaSourceAddress;
    private int dmaByteIndex;
    private boolean dmaDelayPhase;
    private boolean listenerRegistered = false;

    @Inject
    public MemoryBus(@Named("underlying") Memory underlying) {
        this.underlying = underlying;
        this.dmaActive = false;
        this.dmaSourceAddress = 0;
        this.dmaByteIndex = 0;
        this.dmaDelayPhase = false;
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
        dmaActive = true;
        dmaSourceAddress = (short) ((sourceHigh & 0xFF) << 8);
        dmaByteIndex = 0;
        dmaDelayPhase = true;
    }

    @Override
    public byte read(short address) {
        ensureDmaListenerRegistered();
        if (dmaActive && !isAccessibleDuringDma(address)) {
            return (byte) 0xFF;
        }
        return underlying.read(address);
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
        if (!dmaActive) {
            return;
        }

        if (dmaDelayPhase) {
            dmaDelayPhase = false;
            return;
        }

        if (dmaByteIndex < OAM_SIZE) {
            short sourceAddr = (short) (dmaSourceAddress + dmaByteIndex);
            short destAddr = (short) (OAM_START_ADDRESS + dmaByteIndex);
            byte data = underlying.read(sourceAddr);
            underlying.write(destAddr, data);
            dmaByteIndex++;
            if (dmaByteIndex >= OAM_SIZE) {
                dmaActive = false;
            }
        }
    }

    @Override
    public boolean isDmaActive() {
        return dmaActive;
    }

    private boolean isAccessibleDuringDma(short address) {
        int addr = address & 0xFFFF;
        // During DMA, allow access to HRAM (0xFF80-0xFFFE) and I/O registers (0xFF00-0xFF7F)
        return (addr >= 0xFF00 && addr <= 0xFFFE);
    }
}
