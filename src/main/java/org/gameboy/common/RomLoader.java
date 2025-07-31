package org.gameboy.common;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class RomLoader {
    
    public byte[] loadRom(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        return Files.readAllBytes(path);
    }
    
    public byte[] loadRom(String filepath, int maxSize) throws IOException {
        Path path = Paths.get(filepath);
        long fileSize = Files.size(path);
        
        if (fileSize > maxSize) {
            throw new IOException("ROM file exceeds maximum size: " + fileSize + " > " + maxSize);
        }
        
        return Files.readAllBytes(path);
    }
}