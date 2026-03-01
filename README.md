# Game Boy Emulator

A Game Boy (DMG) emulator written in Java. Requires JDK 21+.

## Usage

```bash
./gradlew build
./gradlew run                          # loads first .gb file in roms/
./gradlew run --args="Tetris.gb"       # specify a ROM
```

ROM paths are resolved as: absolute path > current directory > `roms/` directory.

## Supported Cartridges

Detected automatically from the ROM header: ROM Only (0x00), MBC1 (0x01-0x03), MBC3 (0x0F-0x13).
