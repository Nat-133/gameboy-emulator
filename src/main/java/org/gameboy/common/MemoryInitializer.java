package org.gameboy.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MemoryInitializer {
    private final RomLoader romLoader;
    
    @Inject
    public MemoryInitializer(RomLoader romLoader) {
        this.romLoader = romLoader;
    }
    
    public List<MemoryDump> createMemoryDumps(String bootRomPath, String gameRomPath) throws IOException {
        List<MemoryDump> dumps = new ArrayList<>();
        
        byte[] gameRom = romLoader.loadRom(gameRomPath, 0x8000); // 32KB max for basic ROMs
        dumps.add(MemoryDump.fromZero(gameRom));
        
        if (bootRomPath != null) {
            byte[] bootRom = romLoader.loadRom(bootRomPath, 0x100); // Boot ROM is 256 bytes
            dumps.add(MemoryDump.fromZero(bootRom));
        }
        
        return dumps;
    }
}