# Game Boy Emulator

A Game Boy (DMG) emulator written in Java. Requires JDK 21+.

Rendering, input, and audio are handled through LWJGL (GLFW + OpenGL + OpenAL).

## Features

- Cycle-accurate CPU, PPU, and timer
- Full 4-channel audio (APU): two pulse channels, wave channel, noise channel
- Keyboard and on-screen (mouse) input
- Automatic cartridge detection: ROM Only, MBC1, MBC3 (with RTC)

## Usage

```bash
./gradlew build
./gradlew run                          # loads first .gb file in roms/
./gradlew run --args="Tetris.gb"       # specify a ROM
```

ROM paths are resolved as: absolute path > current directory > `roms/` directory.
If no ROM is given, the first `.gb` file in `roms/` is loaded, falling back to the
bundled default ROM.

## Controls

| Game Boy | Keyboard    |
|----------|-------------|
| D-Pad    | W A S D     |
| A        | K           |
| B        | J           |
| Start    | Enter       |
| Select   | Left Shift  |

On-screen buttons can also be operated with the mouse.

## Supported Cartridges

Detected automatically from the ROM header: ROM Only (0x00), MBC1 (0x01-0x03),
MBC3 (0x0F-0x13).

## Testing

```bash
./gradlew test
```
