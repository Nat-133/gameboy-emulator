# Mooneye Test Analysis: boot_hwio-dmgABCmgb

## Test Overview

This test validates the initial values of hardware I/O registers after the boot ROM has completed execution. The test is specifically designed for DMG (Game Boy) models ABC and MGB, and checks that all hardware registers have the correct post-boot initialization values. It skips the wave RAM region ($FF30-$FF3F) as those values are affected by random factors.

## What The Test Does

The test performs a systematic scan of all hardware I/O registers from $FF00 to $FF7F and $FFFF, comparing actual register values against expected values. Here's the step-by-step execution flow:

1. **Initialize pointers**:
   - HL = $FF00 (starting hardware register address)
   - DE = hwio_data (pointer to expected data table)

2. **Main loop** (iterates through memory from $FF00 to $FF7F):
   - Load a mask byte from the data table (indicates which registers in the next 8 bytes should be tested)
   - For each of the next 8 register addresses:
     - Read the actual value from the hardware register at (HL)
     - Check the corresponding bit in the mask byte
     - If the bit is set (1), compare the actual value with the expected value from the data table
     - If values don't match, jump to the mismatch handler
     - Increment to next address
   - Continue until HL reaches $FF80

3. **Special test for $FFFF (IE register)**:
   - Read the value at $FFFF (Interrupt Enable register)
   - Compare it with the expected value of $00
   - If it doesn't match, jump to the mismatch handler

4. **Success path**:
   - If all values match, call `quit_ok` which prints "Test OK" and returns with d=$00

5. **Failure path**:
   - Store the expected value, actual value, and address of the mismatch
   - Print detailed error message showing:
     - "MISMATCH AT $[address]"
     - "EXPECTED [expected_value]"
     - "GOT [actual_value]"
   - Return with d=$42 to indicate failure

## What The Test Expects

The test expects specific post-boot values for hardware registers. Here's the complete mapping based on the hwio_data table:

### $FF00-$FF07: Joypad, Serial, and Timer
- $FF00 (P1): $CF (mask $FF) - Joypad register
- $FF01 (SB): $00 (mask $FF) - Serial data
- $FF02 (SC): $7E (mask $FF) - Serial control
- $FF03: $FF (mask $FF) - Unused
- $FF04 (DIV): $AD (mask $FF) - Divider register
- $FF05 (TIMA): $00 (mask $FF) - Timer counter
- $FF06 (TMA): $00 (mask $FF) - Timer modulo
- $FF07 (TAC): $F8 (mask $FF) - Timer control

### $FF08-$FF0F: Unused and IF
- $FF08-$FF0E: $FF (mask $FF for all) - Unused
- $FF0F (IF): $E1 (mask $FF) - Interrupt flags

### $FF10-$FF17: Audio Channel 1 and 2
- $FF10 (NR10): $80 (mask $FF) - Channel 1 sweep
- $FF11 (NR11): $BF (mask $FF) - Channel 1 length/duty
- $FF12 (NR12): $F3 (mask $FF) - Channel 1 envelope
- $FF13 (NR13): $FF (mask $FF) - Channel 1 frequency lo
- $FF14 (NR14): $BF (mask $FF) - Channel 1 frequency hi
- $FF15: $FF (mask $FF) - Unused
- $FF16 (NR21): $3F (mask $FF) - Channel 2 length/duty
- $FF17 (NR22): $00 (mask $FF) - Channel 2 envelope

### $FF18-$FF1F: Audio Channel 2 (cont) and 3
- $FF18 (NR23): $FF (mask $FF) - Channel 2 frequency lo
- $FF19 (NR24): $BF (mask $FF) - Channel 2 frequency hi
- $FF1A (NR30): $7F (mask $FF) - Channel 3 on/off
- $FF1B (NR31): $FF (mask $FF) - Channel 3 length
- $FF1C (NR32): $9F (mask $FF) - Channel 3 output level
- $FF1D (NR33): $FF (mask $FF) - Channel 3 frequency lo
- $FF1E (NR34): $BF (mask $FF) - Channel 3 frequency hi
- $FF1F: $FF (mask $FF) - Unused

### $FF20-$FF27: Audio Channel 4 and Master
- $FF20 (NR41): $FF (mask $FF) - Channel 4 length
- $FF21 (NR42): $00 (mask $FF) - Channel 4 envelope
- $FF22 (NR43): $00 (mask $FF) - Channel 4 polynomial
- $FF23 (NR44): $BF (mask $FF) - Channel 4 control
- $FF24 (NR50): $77 (mask $FF) - Master volume
- $FF25 (NR51): $F3 (mask $FF) - Sound panning
- $FF26 (NR52): $F1 (mask $FF) - Sound on/off
- $FF27: $FF (mask $FF) - Unused

### $FF28-$FF2F: Unused
- $FF28-$FF2F: $FF (mask $FF for all) - All unused

### $FF30-$FF3F: Wave RAM (SKIPPED)
- $FF30-$FF37: NOT TESTED (mask $00) - Wave pattern RAM
- $FF38-$FF3F: NOT TESTED (mask $00) - Wave pattern RAM

### $FF40-$FF47: LCD Control and Scroll
- $FF40 (LCDC): $91 (mask $FF) - LCD control
- $FF41 (STAT): $80 (mask $FD, bit 1 ignored) - LCD status
- $FF42 (SCY): $00 (mask $FF) - Scroll Y
- $FF43 (SCX): $00 (mask $FF) - Scroll X
- $FF44 (LY): $0A (mask $FF) - LCD Y coordinate
- $FF45 (LYC): $00 (mask $FF) - LY compare
- $FF46 (DMA): $FF (mask $FF) - OAM DMA
- $FF47 (BGP): $FC (mask $FF) - BG palette

### $FF48-$FF4F: Sprite Palettes and Other
- $FF48 (OBP0): $FF (mask $3F, only bits 0-5 tested) - Sprite palette 0
- $FF49 (OBP1): $FF (mask $3F, only bits 0-5 tested) - Sprite palette 1
- $FF4A (WY): $00 (mask $FF) - Window Y
- $FF4B (WX): $00 (mask $FF) - Window X
- $FF4C-$FF4F: $FF (mask $FF for all) - Various/unused

### $FF50-$FF7F: Boot ROM disable and High RAM
- $FF50 onwards: $FF (mask $FF for all) - Boot disable register and HRAM area

### $FFFF: Interrupt Enable
- $FFFF (IE): $00 (mask implied $FF) - Interrupt enable

## What The Test Is Testing

This test validates that the **hardware register initialization after boot ROM completion** is correct. Specifically:

1. **Post-Boot State**: The test runs AFTER the boot ROM has executed, so it's checking the state that the boot ROM leaves the hardware in.

2. **I/O Register Initialization**: Each hardware component should have specific initialization values:
   - Joypad register should be properly configured
   - Serial transfer should be in a known state
   - Timer registers (DIV, TIMA, TMA, TAC) should have specific values
   - Audio registers should be initialized to their post-boot state
   - LCD/PPU registers should reflect the state after boot
   - Interrupt registers should be in the correct state

3. **DMG-Specific Behavior**: The test is designed for DMG models ABC and MGB specifically, which have different initialization values than DMG 0 or Game Boy Color models.

## Potential Failure Reasons

Based on analysis of the emulator code at `/Users/nathaniel.manley/vcs/personal/gameboy-emulator`, here are the likely causes of test failure:

### 1. Missing Audio Register Implementation

**Problem**: The emulator does not implement any audio (sound) registers (NR10-NR52).

**Evidence**:
- No audio-related code found in the codebase
- `MappedMemory.java` does not map any audio register addresses ($FF10-$FF26)
- Grep searches for NR10, NR11, etc. return no results

**Impact**: The test expects these registers to return specific values:
- $FF10 (NR10): $80
- $FF11 (NR11): $BF
- $FF12 (NR12): $F3
- $FF14 (NR14): $BF
- $FF16 (NR21): $3F
- $FF17 (NR22): $00
- $FF1A (NR30): $7F
- $FF1C (NR32): $9F
- $FF1E (NR34): $BF
- $FF23 (NR44): $BF
- $FF24 (NR50): $77
- $FF25 (NR51): $F3
- $FF26 (NR52): $F1

Without these registers implemented, reads from these addresses will likely return $00 or $FF (depending on the default memory value), causing mismatches.

### 2. Incorrect P1 (Joypad) Register Initialization

**Problem**: The P1 register at $FF00 is not mapped in `MappedMemory.java`.

**Expected**: $CF
**Likely Actual**: $00 or $FF (unmapped memory)

**Evidence**: No joypad controller or P1 register mapping found in `MappedMemory.java` constructor.

### 3. Incorrect Serial Registers (SB/SC) Initialization

**Problem**: Serial registers are mapped but may not have correct post-boot values.

**Expected**:
- $FF01 (SB): $00
- $FF02 (SC): $7E

**Evidence**: In `SerialController.java`:
```java
private byte serialData = 0;      // This is correct for SB
private byte serialControl = 0;   // This should be $7E, not $00
```

The serial control register initializes to $00 but should be $7E after boot.

### 4. Incorrect DIV Register Value

**Problem**: DIV register should read as $AD after boot.

**Expected**: $AD
**Likely Actual**: Depends on `InternalTimerCounter` initialization

**Evidence**: In `CoreModule.java`:
```java
InternalTimerCounter provideInternalTimerCounter() {
    return new InternalTimerCounter(0xAAC8);
}
```

This initializes the internal counter to $AAC8, which means DIV (upper byte) would be $AA, not $AD. The comment says it's aligned for boot_sclk_align test, but this may not match the boot_hwio test requirements.

### 5. TAC Register Value - CORRECT

**Status**: TAC register is correctly initialized to $F8.

**Expected**: $F8
**Actual**: $F8 (CORRECT)

**Evidence**: In `TacRegister.java`:
```java
public TacRegister() {
    this.value = new AtomicInteger(0xF8);
}

public byte read() {
    return (byte) (value.get() | UPPER_BITS_MASK);  // UPPER_BITS_MASK = 0xF8
}
```

The TAC register correctly initializes to $F8 and forces upper bits to be set on read, so this register should pass the test.

### 6. Incorrect IF (Interrupt Flags) Register Value

**Problem**: IF register should read as $E1 after boot, but the emulator initializes it to $E0.

**Expected**: $E1
**Likely Actual**: $E0

**Evidence**: In `InterruptFlagsRegister.java`:
```java
private final AtomicInteger value = new AtomicInteger(0);

public InterruptFlagsRegister() {
    this.value.set(0);  // Initializes to 0
}

public byte read() {
    return (byte) (value.get() | UPPER_BITS_MASK);  // UPPER_BITS_MASK = 0xE0
}
```

The register initializes to $00, and when read, it OR's with $E0, resulting in $E0. However, the test expects $E1 (which would mean bit 0 should be set to indicate a VBLANK interrupt pending state). This is a post-boot state that the emulator doesn't simulate correctly.

### 7. Incorrect LY Register Value

**Problem**: LY register should read as $0A after boot.

**Expected**: $0A
**Likely Actual**: $00 (default initialization)

**Evidence**: In `DisplayModule.java`:
```java
ByteRegister provideLyRegister() {
    return new IntBackedRegister();  // Defaults to 0
}
```

LY should be initialized to $0A to match the post-boot state, but it defaults to $00.

### 8. Incorrect STAT Register Value

**Problem**: STAT register should read as $80 (with bit 1 ignored in comparison).

**Expected**: $80 (masked with $FD, so bit 1 is don't-care)
**Likely Actual**: $85 (from initialization) → reads as $85 | $80 = $85

**Evidence**: In `DisplayModule.java`:
```java
ByteRegister provideStatRegister() {
    return new StatRegister(0x85);
}
```

And in `StatRegister.java`:
```java
public byte read() {
    return (byte) (value | 0x80);  // Bit 7 always returns 1
}
```

If initialized to $85, it will read as $85 (since bit 7 is already set). The test expects $80 when masked with $FD. Since $85 & $FD = $85 and $80 & $FD = $80, these don't match.

### 9. Missing Mappings for Many Registers

**Problem**: Many register addresses are not mapped in `MappedMemory.java`.

**Unmapped addresses** that should have specific values:
- $FF03, $FF08-$FF0E (unused regions expecting $FF)
- $FF13, $FF15, $FF18, $FF1D (audio registers)
- $FF1F, $FF27-$FF2F (unused regions)
- $FF4C-$FF4F, $FF50-$FF7F (various/HRAM)

These will return whatever the default memory contains (likely $00 or $FF depending on ROM content), which may not match expected values.

### 10. Incorrect DMA Register Value

**Problem**: DMA register should read as $FF after boot.

**Expected**: $FF
**Likely Actual**: $00 (default initialization)

**Evidence**: In `CoreModule.java`:
```java
ByteRegister provideDmaRegister() {
    return new IntBackedRegister();  // Defaults to 0
}
```

### 11. Incorrect Default Memory Initialization

**Problem**: Unused register addresses should return $FF.

**Evidence**: In `MappedMemory.java`:
```java
private final byte[] defaultMemory = new byte[0x10000];
```

Java initializes arrays to $00 by default, but many unused hardware register addresses should return $FF when read.

## Summary

The emulator will fail this test primarily because:

1. **No audio hardware implementation** (CRITICAL)
   - All NR registers ($FF10-$FF26) are completely missing
   - This accounts for 17 registers with wrong values

2. **No joypad controller** (CRITICAL)
   - P1 register ($FF00) not mapped

3. **Incorrect post-boot initialization values** for:
   - Serial control (SC) register: $00 instead of $7E
   - DIV register: $AA instead of $AD (due to internal counter being $AAC8 instead of $ADxx)
   - IF register: $E0 instead of $E1 (missing VBLANK interrupt flag)
   - LY register: $00 instead of $0A
   - STAT register: $85 instead of $80 (when masked with $FD)
   - DMA register: $00 instead of $FF

4. **Unmapped unused register regions** that should return $FF but will return $00
   - $FF03, $FF08-$FF0E, $FF15, $FF1F, $FF27-$FF2F
   - $FF4C-$FF4F, $FF50-$FF7F

5. **Missing proper post-boot state setup**
   - The emulator doesn't simulate the boot ROM execution
   - Hardware is not left in the correct post-boot state

### Correctly Implemented Registers

The following registers ARE correctly initialized:
- TAC register ($FF07): Correctly returns $F8
- LCDC register ($FF40): Correctly initialized to $91
- BGP register ($FF47): Correctly initialized to $FC
- OBP0/OBP1 registers ($FF48/$FF49): Correctly initialized to $FF
- TIMA, TMA registers ($FF05/$FF06): Correctly initialized to $00
- SCY, SCX, LYC, WY, WX registers: Correctly initialized to $00
- IE register ($FFFF): Correctly initialized to $00

### To Fix This Test

The emulator would need:
1. Full audio register implementation with proper post-boot initialization values for all NR registers
2. Joypad controller with P1 register mapping (initialized to $CF)
3. Adjust initialization values for SC, DIV, IF, LY, STAT, and DMA registers
4. Proper handling of unmapped I/O regions to return $FF instead of $00
5. Either boot ROM emulation or explicit post-boot state initialization for all hardware components
