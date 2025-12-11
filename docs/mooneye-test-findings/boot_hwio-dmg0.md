# Mooneye Test Analysis: boot_hwio-dmg0

## Test Overview

The `boot_hwio-dmg0` test verifies the initial power-on state of all hardware I/O registers (memory range $FF00-$FF7F and $FFFF) for the original DMG-0 (DMG revision 0) Game Boy hardware. This test is specific to DMG-0 and will fail on all other Game Boy models (DMG ABC, MGB, SGB, SGB2, CGB, AGB, AGS) since they have different initial register values.

## What The Test Does

The test performs a systematic scan of all hardware I/O registers from $FF00 to $FF7F, plus the interrupt enable register at $FFFF:

1. **Initialize pointers**:
   - HL = $FF00 (starting I/O address)
   - DE = hwio_data (pointer to expected values table)

2. **For each 8-byte block from $FF00 to $FF7F**:
   - Read a mask byte from the data table
   - For each of the 8 bits in the mask (high bit to low bit):
     - If the mask bit is 1: Read the hardware register, compare with expected value from table, fail if mismatch
     - If the mask bit is 0: Skip this register (not tested, typically wave RAM at $FF30-$FF3F)
     - Increment to next register address
   - Continue until HL reaches $FF80

3. **Extra test for $FFFF (IE register)**:
   - Read the interrupt enable register
   - Expect it to be $00
   - Fail if mismatch

4. **On success**: Call `quit_ok` which prints "Test OK" and returns with D=$00
5. **On failure**: Call `mismatch` handler which prints the failing address and expected vs actual values

## What The Test Expects

The test expects the following initial values for DMG-0 hardware immediately after power-on:

### Joypad and Serial ($FF00-$FF07)
- $FF00 (P1 - Joypad): $CF
- $FF01 (SB - Serial data): $00
- $FF02 (SC - Serial control): $7E
- $FF03 (unused): $FF
- $FF04 (DIV - Divider): $19 (or possibly $AB based on timing)
- $FF05 (TIMA - Timer counter): $00
- $FF06 (TMA - Timer modulo): $00
- $FF07 (TAC - Timer control): $F8

### Unused ($FF08-$FF0E)
- $FF08-$FF0E: All $FF (unused registers)

### Interrupt Flag ($FF0F)
- $FF0F (IF - Interrupt flag): $E1

### Sound Registers ($FF10-$FF26)
- $FF10 (NR10): $80
- $FF11 (NR11): $BF
- $FF12 (NR12): $F3
- $FF13 (NR13): $FF
- $FF14 (NR14): $BF
- $FF15 (unused): $FF
- $FF16 (NR21): $3F
- $FF17 (NR22): $00
- $FF18 (NR23): $FF
- $FF19 (NR24): $BF
- $FF1A (NR30): $7F
- $FF1B (NR31): $FF
- $FF1C (NR32): $9F
- $FF1D (NR33): $FF
- $FF1E (NR34): $BF
- $FF1F (unused): $FF
- $FF20 (NR41): $FF
- $FF21 (NR42): $00
- $FF22 (NR43): $00
- $FF23 (NR44): $BF
- $FF24 (NR50): $77
- $FF25 (NR51): $F3
- $FF26 (NR52): $F1
- $FF27-$FF2F (unused): All $FF

### Wave RAM ($FF30-$FF3F)
- NOT TESTED (mask = %00000000) - Wave RAM has random values at power-on

### PPU Registers ($FF40-$FF4B)
- $FF40 (LCDC): $91
- $FF41 (STAT): $83 (read-only, note: when read bit 7 is always 1, so reads as $83)
- $FF42 (SCY): $00
- $FF43 (SCX): $00
- $FF44 (LY): $01 (NOT $00 - LCD is already active at scanline 1)
- $FF45 (LYC): $00
- $FF46 (DMA): $FF
- $FF47 (BGP): $FC
- $FF48 (OBP0): $FF (test expects $00 but may be incorrect)
- $FF49 (OBP1): $FF (test expects $00 but may be incorrect)
- $FF4A (WY): $00
- $FF4B (WX): $00
- $FF4C-$FF4F (unused/CGB only): All $FF

### More Unused ($FF50-$FF7F)
- $FF50-$FF7F: All $FF (unused or CGB-only registers)

### Interrupt Enable ($FFFF)
- $FFFF (IE): $00

## What The Test Is Testing

This test validates that the emulator correctly implements the **power-on state** of the DMG-0 Game Boy hardware. Specifically:

1. **Hardware initialization**: All I/O registers must have their correct DMG-0 power-on values
2. **Sound hardware state**: Audio registers must be initialized correctly (e.g., NR52=$F1 means sound is on)
3. **PPU state**: Display must be enabled (LCDC=$91) and already running (LY=$01, not $00)
4. **Timer state**: DIV register must have advanced some amount from power-on
5. **Interrupt state**: No interrupts enabled (IE=$00) but some flags may be set (IF=$E1)
6. **Unused registers**: Must read as $FF

This test would typically run immediately after the boot ROM completes (or when skipping the boot ROM), testing that the emulator's post-boot state matches real hardware.

## Potential Failure Reasons

### 1. Missing Audio/Sound Register Implementation

**Issue**: The emulator has NO sound/audio hardware implementation.

**Evidence**: No files found matching audio, sound, NR10, NR11, NR12, or related patterns in the codebase.

**Impact**: ALL sound registers ($FF10-$FF26) will fail the test:
- Expected values: $80, $BF, $F3, $FF, $BF, $FF, $3F, $00, etc.
- Actual values: Likely all $00 or $FF depending on how unmapped registers are handled

**Fix needed**: Implement audio processing unit (APU) with proper initial register values, or at minimum ensure these addresses return the correct power-on values even without functional audio.

### 2. Missing Joypad (P1) Register

**Issue**: No joypad/input handling found at $FF00.

**Evidence**: MappedMemory.java doesn't map address $FF00 to any register. No joypad controller found in the codebase.

**Impact**:
- Expected: $FF00 = $CF
- Actual: Likely $00 or $FF (default memory value)

**Fix needed**: Implement a JoypadRegister that returns $CF when no buttons are pressed, and properly handles the column selection bits.

### 3. Incorrect Initial Values for PPU Palette Registers

**Issue**: OBP0 and OBP1 initialized to $FF instead of $00.

**Evidence**:
```java
// DisplayModule.java lines 119-127
@Named("obp0")
ByteRegister provideObp0Register() {
    return new IntBackedRegister(0xFF);  // Should be 0x00 for DMG-0
}

@Named("obp1")
ByteRegister provideObp1Register() {
    return new IntBackedRegister(0xFF);  // Should be 0x00 for DMG-0
}
```

**Impact**:
- Expected: $FF48 (OBP0) = $00, $FF49 (OBP1) = $00
- Actual: $FF48 = $FF, $FF49 = $FF

**Note**: The test data shows $00 for these registers but other documentation suggests they may actually be $FF. Need to verify against real DMG-0 hardware. The mask is %00111111 meaning only registers $FF48-$FF49 are tested in that block, not $FF4C-$FF4F.

### 4. Incorrect STAT Register Initial Value

**Issue**: STAT initialized to $85 instead of $83.

**Evidence**:
```java
// DisplayModule.java line 106
@Named("stat")
ByteRegister provideStatRegister() {
    return new StatRegister(0x85);  // Should be 0x83 for DMG-0
}
```

**Impact**:
- Expected: $FF41 = $83 (when read, bit 7 is set so this is the read value)
- Actual: $FF41 = $85 (when read, becomes $85 | $80 = $85)

**Analysis**:
- Internal value $83 = %10000011 (bit 7 set, mode 3, coincidence on)
- Internal value $85 = %10000101 (bit 7 set, mode 1, coincidence on)
- The initial STAT mode should be mode 3 (OAM scanning) not mode 1 (V-blank)
- However, bit 2 (coincidence flag) depends on whether LY=LYC at power-on

### 5. Incorrect LY Register Initial Value

**Issue**: LY likely initialized to $00 instead of $01.

**Evidence**:
```java
// DisplayModule.java line 55
@Named("ly")
ByteRegister provideLyRegister() {
    return new IntBackedRegister();  // Defaults to 0
}
```

**Impact**:
- Expected: $FF44 = $01
- Actual: $FF44 = $00

**Analysis**: At power-on, the DMG-0 PPU is already running and has advanced to scanline 1. The emulator initializes LY to 0, which only happens at the very beginning of a frame.

### 6. Incorrect IF (Interrupt Flag) Initial Value

**Issue**: IF register initialized to $E0 instead of $E1.

**Evidence**:
```java
// InterruptFlagsRegister.java lines 8-17
private static final int UPPER_BITS_MASK = 0xE0;
private final AtomicInteger value = new AtomicInteger(0);

@Override
public byte read() {
    return (byte) (value.get() | UPPER_BITS_MASK);
}
```

**Impact**:
- Expected: $FF0F = $E1 = %11100001 (V-blank interrupt flag set, plus unused bits)
- Actual: $FF0F = $E0 = %11100000 (no interrupt flags set, only unused bits)

**Analysis**: The IF register correctly sets the upper 3 bits (5-7) to 1 when read (unused bits), but initializes the internal value to 0, giving $E0. DMG-0 expects bit 0 (V-blank interrupt) to be set at power-on, giving $E1. The internal value should be initialized to 1 instead of 0.

### 7. Serial Control Register Wrong Initial Value

**Issue**: SC (Serial Control) likely returns $00 or $7F instead of $7E.

**Evidence**: SerialController initializes serialControl to 0:
```java
// SerialController.java line 12
private byte serialControl = 0;
```

**Impact**:
- Expected: $FF02 = $7E
- Actual: $FF02 = $00 or possibly $7F if high bit is forced

**Analysis**: $7E = %01111110, which is an unusual value. May indicate some internal serial state at power-on.

### 8. DIV Register Initial Value

**Issue**: DIV timing-dependent value may not match $19.

**Evidence**:
```java
// CoreModule.java lines 53-55
InternalTimerCounter provideInternalTimerCounter() {
    return new InternalTimerCounter(0xAAC8);  // This should give DIV=$AA, not $19
}
```

**Impact**:
- Expected: $FF04 = $19
- Actual: $FF04 = $AA

**Analysis**: The internal counter value $AAC8 was chosen for boot_sclk_align test but gives the wrong DIV value for DMG-0 power-on state. DIV=$19 suggests internal counter should be around $19xx (exact value depends on lower bits).

### 9. Missing Boot ROM Post-State Setup

**Issue**: The emulator may not be properly simulating the state AFTER the boot ROM completes.

**Evidence**: Main.java allows skipping the boot ROM but doesn't set up post-boot register states when bootRomPath is null.

**Analysis**: This test expects the state after the DMG boot ROM has finished executing (when $FF50 is written to disable the boot ROM). The boot ROM:
- Initializes sound registers
- Sets up the PPU (LCDC=$91, enables display)
- Advances timers/counters
- Sets some interrupt flags

If the emulator skips the boot ROM, it needs to manually set all registers to their post-boot values for DMG-0 hardware.

### 10. Default Memory Values for Unmapped Registers

**Issue**: Unmapped I/O registers may return $00 instead of $FF.

**Evidence**:
```java
// MappedMemory.java line 19
private final byte[] defaultMemory = new byte[0x10000];  // Initialized to 0
```

**Impact**: Any unmapped I/O register will return $00 instead of the expected $FF.

**Fix needed**: Initialize the I/O region ($FF00-$FF7F) of defaultMemory to $FF before mapping specific registers.

## Summary

The test will likely fail on multiple registers due to:

1. **Complete absence of audio hardware** (16+ register failures)
2. **Missing joypad register** (1 failure)
3. **Incorrect PPU register initial values** (multiple failures for STAT, LY, potentially OBP0/OBP1)
4. **Incorrect timer initial values** (DIV wrong value)
5. **Incorrect serial and interrupt flag initial values**
6. **Unmapped registers returning $00 instead of $FF**

To pass this test, the emulator needs a comprehensive initialization system that sets all DMG-0 power-on register values, either by running the actual boot ROM or by manually initializing all registers to their correct post-boot state.
