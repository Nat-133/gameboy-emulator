package org.gameboy.cpu;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.gameboy.common.Clock;
import org.gameboy.common.ClockWithParallelProcess;
import org.gameboy.common.Memory;
import org.gameboy.components.Timer;
import org.gameboy.cpu.annotations.Prefixed;
import org.gameboy.cpu.annotations.Unprefixed;
import org.gameboy.cpu.components.*;
import org.gameboy.display.PictureProcessingUnit;

public class CpuModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ArithmeticUnit.class).in(Singleton.class);
        bind(IncrementDecrementUnit.class).in(Singleton.class);
        
        bind(OpcodeTable.class).annotatedWith(Unprefixed.class)
            .to(UnprefixedOpcodeTable.class).in(Singleton.class);
        bind(OpcodeTable.class).annotatedWith(Prefixed.class)
            .to(PrefixedOpcodeTable.class).in(Singleton.class);
    }
    
    @Provides
    CpuRegisters provideCpuRegisters() {
        return new CpuRegisters(
            (short) 0x0000,  // af
            (short) 0x0000,  // bc
            (short) 0x0000,  // de
            (short) 0x0000,  // hl
            (short) 0x0000,  // sp
            (short) 0x0100,  // pc - starts at 0x100 after boot ROM
            (byte) 0x00,     // instructionRegister
            true             // ime - interrupts enabled initially
        );
    }
    
    @Provides
    Decoder provideDecoder(@Unprefixed OpcodeTable unprefixedTable, @Prefixed OpcodeTable prefixedTable) {
        return new Decoder(unprefixedTable, prefixedTable);
    }
    
    @Provides
    @Singleton
    @Named("cpuClock")
    Clock provideCpuClock(Provider<PictureProcessingUnit> ppuProvider, Provider<Timer> timerProvider) {
        Timer timer = timerProvider.get();
        PictureProcessingUnit ppu = ppuProvider.get();
        return new ClockWithParallelProcess(() -> {
            timer.mCycle();
            for (int i = 0; i < 4; i++) {
                ppu.tCycle();
            }
        });
    }
    
    @Provides
    CpuStructure provideCpuStructure(CpuRegisters registers, Memory memory, ArithmeticUnit alu,
                                     IncrementDecrementUnit idu, @Named("cpuClock") Clock clock,
                                     Decoder decoder) {
        // Create InterruptBus with the same clock as CPU (matching test setup)
        InterruptBus interruptBus = new InterruptBus(memory, clock);
        return new CpuStructure(registers, memory, alu, idu, clock, interruptBus, decoder);
    }
    
    @Provides
    @Singleton
    Cpu provideCpu(CpuStructure cpuStructure) {
        return new Cpu(cpuStructure);
    }
}