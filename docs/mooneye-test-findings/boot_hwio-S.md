# Mooneye Test Analysis: boot_hwio-S

## Test Overview

The `boot_hwio-S` test verifies the initial values of hardware I/O registers after boot on Super Game Boy (SGB/SGB2) hardware. The test systematically reads all hardware I/O registers from 0xFF00 to 0xFF7F (excluding volatile registers) and compares them against expected initial values specific to SGB hardware.

**Important Context:** This test is designed to PASS on SGB/SGB2 and FAIL on DMG/MGB/CGB/AGB/AGS hardware. Each Game Boy variant has different initial register values after boot.

## What The Test Does

The test performs a systematic scan of the hardware I/O register space using a mask-based approach:

### Step-by-Step Execution Flow

1. **Initialize Pointers:**
   - HL = 0xFF00 (starting memory address)
   - DE = hwio_data (pointer to test data table)

2. **For each 8-byte block from 0xFF00 to 0xFF7F:**
   - Read mask byte from test data (indicates which registers to check)
   - For each of the 8 registers in the block:
     - Check if the corresponding bit in the mask is set
     - If set (bit = 1): Read expected value and compare with actual memory value
     - If not set (bit = 0): Skip this register (volatile or timing-dependent)
     - Increment to next register
   - Continue until HL reaches 0xFF80

3. **Special Check for Interrupt Enable (IE):**
   - Read 0xFFFF (IE register)
   - Expected value: 0x00
   - Compare and fail if mismatch

4. **Result Handling:**
   - If all values match: Display "Test OK" and exit with success (D=0x00)
   - If any mismatch: Display detailed mismatch info and exit with failure (D=0x42)

### Registers Excluded from Testing

The following registers are skipped because they're volatile or timing-dependent:
- **0xFF04 (DIV):** Constantly incrementing divider register
- **0xFF30-0xFF3F:** Wave RAM (can have unpredictable initial state)
- **0xFF40 (LCDC):** LCD Control (can vary based on timing)
- **0xFF41 (STAT):** LCD Status (depends on PPU state/timing)

### Mask Interpretation

The mask byte uses bits 7-0 to indicate which of the next 8 registers should be checked:
- Bit 7 = 1: Check first register (offset +0)
- Bit 6 = 1: Check second register (offset +1)
- ...
- Bit 0 = 1: Check eighth register (offset +7)

## What The Test Expects (SGB/SGB2 Values)

The test expects the following initial register values for SGB/SGB2 hardware:

### Memory Range 0xFF00-0xFF07 (Mask: %11110111)
- **0xFF00 (P1/JOYP):** 0xFF - Joypad register (all buttons released, all directions released)
- **0xFF01 (SB):** 0x00 - Serial transfer data
- **0xFF02 (SC):** 0x7E - Serial transfer control
- **0xFF03:** (Skip - unmapped/unused)
- **0xFF04 (DIV):** (Skip - volatile)
- **0xFF05 (TIMA):** 0xFF - Timer counter
- **0xFF06 (TMA):** 0x00 - Timer modulo
- **0xFF07 (TAC):** 0xF8 - Timer control

### Memory Range 0xFF08-0xFF0F (Mask: %11111111)
- **0xFF08-0xFF0E:** 0xFF - Unused registers
- **0xFF0F (IF):** 0xE1 - Interrupt flags (upper 3 bits set, bit 0 set)

### Memory Range 0xFF10-0xFF17 (Mask: %11111111) - Audio Channel 1
- **0xFF10 (NR10):** 0x80 - Channel 1 sweep
- **0xFF11 (NR11):** 0xBF - Channel 1 length/wave duty
- **0xFF12 (NR12):** 0xF3 - Channel 1 volume envelope
- **0xFF13 (NR13):** 0xFF - Channel 1 frequency low
- **0xFF14 (NR14):** 0xBF - Channel 1 frequency high/control
- **0xFF15:** 0xFF - Unused
- **0xFF16 (NR21):** 0x3F - Channel 2 length/wave duty
- **0xFF17 (NR22):** 0x00 - Channel 2 volume envelope

### Memory Range 0xFF18-0xFF1F (Mask: %11111111) - Audio Channels 2 & 3
- **0xFF18 (NR23):** 0xFF - Channel 2 frequency low
- **0xFF19 (NR24):** 0xBF - Channel 2 frequency high/control
- **0xFF1A (NR30):** 0x7F - Channel 3 on/off
- **0xFF1B (NR31):** 0xFF - Channel 3 length
- **0xFF1C (NR32):** 0x9F - Channel 3 output level
- **0xFF1D (NR33):** 0xFF - Channel 3 frequency low
- **0xFF1E (NR34):** 0xBF - Channel 3 frequency high/control
- **0xFF1F:** 0xFF - Unused

### Memory Range 0xFF20-0xFF27 (Mask: %11111111) - Audio Channel 4 & Control
- **0xFF20 (NR41):** 0xFF - Channel 4 length
- **0xFF21 (NR42):** 0x00 - Channel 4 volume envelope
- **0xFF22 (NR43):** 0x00 - Channel 4 polynomial counter
- **0xFF23 (NR44):** 0xBF - Channel 4 control
- **0xFF24 (NR50):** 0x77 - Master volume & VIN panning
- **0xFF25 (NR51):** 0xF3 - Sound output terminal selection
- **0xFF26 (NR52):** 0xF0 - Sound on/off (bit 7 set, channels status)
- **0xFF27:** 0xFF - Unused

### Memory Range 0xFF28-0xFF2F (Mask: %11111111)
- **0xFF28-0xFF2F:** 0xFF - All unused registers

### Memory Range 0xFF30-0xFF37 (Mask: %00000000)
- **Wave RAM:** All skipped (volatile/unpredictable initial state)

### Memory Range 0xFF38-0xFF3F (Mask: %00000000)
- **Wave RAM:** All skipped (volatile/unpredictable initial state)

### Memory Range 0xFF40-0xFF47 (Mask: %00110101)
- **0xFF40 (LCDC):** (Skip - volatile)
- **0xFF41 (STAT):** (Skip - volatile)
- **0xFF42 (SCY):** 0x00 - Scroll Y
- **0xFF43 (SCX):** 0x00 - Scroll X
- **0xFF44 (LY):** (Skip - volatile)
- **0xFF45 (LYC):** 0xFF - LY Compare
- **0xFF46 (DMA):** (Skip - not checked in this block)
- **0xFF47 (BGP):** 0xFC - Background palette

### Memory Range 0xFF48-0xFF4F (Mask: %00111111)
- **0xFF48 (OBP0):** (Skip - not checked)
- **0xFF49 (OBP1):** (Skip - not checked)
- **0xFF4A (WY):** 0x00 - Window Y
- **0xFF4B (WX):** 0x00 - Window X
- **0xFF4C:** 0xFF - Unused/CGB-only
- **0xFF4D (KEY1):** 0xFF - CGB speed switch (unused on SGB)
- **0xFF4E:** 0xFF - Unused
- **0xFF4F (VBK):** 0xFF - CGB VRAM bank (unused on SGB)

### Memory Range 0xFF50-0xFF7F (Mask: %11111111 for all)
- **All registers:** 0xFF - Unused or CGB-only registers

### Special Register 0xFFFF (IE)
- **0xFFFF (IE):** 0x00 - Interrupt enable (all interrupts disabled)

## What The Test Is Testing

This test validates that the emulator correctly initializes hardware I/O registers to their power-on state for **Super Game Boy (SGB/SGB2)** hardware. Specifically, it tests:

1. **Serial Communication State:** SB=0x00, SC=0x7E
2. **Timer Initial State:** TIMA=0xFF, TMA=0x00, TAC=0xF8 (bits masked)
3. **Interrupt State:** IF=0xE1 (upper bits set), IE=0x00
4. **Audio Register Initialization:** All NRxx registers have specific initial patterns
5. **PPU Register Initialization:** Scroll registers at 0x00, LYC=0xFF, BGP=0xFC
6. **Window Position:** WY=0x00, WX=0x00
7. **Unused Register State:** Most unused registers initialized to 0xFF
8. **CGB-Specific Registers:** On SGB, CGB-only registers (KEY1, VBK, etc.) should be 0xFF

## Potential Failure Reasons

### 1. Wrong Hardware Model Initialization

**Issue:** The emulator likely initializes registers to DMG (original Game Boy) values instead of SGB values.

**Evidence in Emulator Code:**

The test explicitly states it's for SGB/SGB2 and expects to FAIL on DMG/MGB/CGB/AGB/AGS. Since the emulator is likely configured for DMG compatibility, it will have different initial values.

### 2. Serial Controller Initialization

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/SerialController.java`

**Current State:**
```java
private byte serialData = 0;      // Correct for SGB (0x00)
private byte serialControl = 0;    // WRONG - should be 0x7E for SGB
```

**Issue:** SC register initializes to 0x00 instead of 0x7E for SGB hardware.

### 3. Timer Register Initialization

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/CoreModule.java`

**Current State:**
```java
@Tima ByteRegister: new IntBackedRegister() // Default 0 (0x00)
@Tma ByteRegister: new IntBackedRegister()  // Default 0 (0x00)
@Tac TacRegister: new TacRegister()         // Unknown initial value
```

**Issue:**
- TIMA should initialize to 0xFF for SGB (currently 0x00)
- TMA is correct at 0x00
- TAC value needs verification (should read as 0xF8 with bit masking)

### 4. Interrupt Flags Register

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/InterruptFlagsRegister.java`

**Current State:**
```java
public InterruptFlagsRegister() {
    this.value.set(0);  // Initializes to 0x00
}

@Override
public byte read() {
    return (byte) (value.get() | UPPER_BITS_MASK);  // Returns 0xE0 initially
}
```

**Issue:** IF register returns 0xE0 initially, but SGB expects 0xE1 (bit 0 should be set).

### 5. PPU Register Initialization

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/display/DisplayModule.java`

**Current State:**
```java
@Named("lcdc"): new IntBackedRegister(0x91)  // Not checked by test
@Named("stat"): new StatRegister(0x85)        // Not checked by test
@Named("scy"): new IntBackedRegister()        // 0x00 - Correct
@Named("scx"): new IntBackedRegister()        // 0x00 - Correct
@Named("ly"): new IntBackedRegister()         // Not checked by test
@Named("lyc"): new IntBackedRegister()        // 0x00 - WRONG, should be 0xFF for SGB
@Named("wy"): new IntBackedRegister()         // 0x00 - Correct
@Named("wx"): new IntBackedRegister()         // 0x00 - Correct
@Named("bgp"): new IntBackedRegister(0xFC)    // 0xFC - Correct
@Named("obp0"): new IntBackedRegister(0xFF)   // Not checked in expected range
@Named("obp1"): new IntBackedRegister(0xFF)   // Not checked in expected range
```

**Issue:** LYC register should initialize to 0xFF for SGB (currently 0x00).

### 6. Interrupt Enable Register

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/CoreModule.java`

**Current State:**
```java
@InterruptEnable ByteRegister: new IntBackedRegister()  // Default 0x00 - Correct
```

**Issue:** This is correct (0x00) for SGB.

### 7. Audio Registers Not Implemented

**Issue:** The emulator does not appear to have audio (APU) registers implemented or mapped in MappedMemory. All NR10-NR52 registers (0xFF10-0xFF26) would return default memory values (likely 0x00) instead of the expected SGB initial values.

**Evidence:** In `MappedMemory.java`, there are no mappings for audio registers (0xFF10-0xFF26).

### 8. Unused Register Initialization

**Issue:** The emulator's default memory initializes to 0x00 (Java byte array default), but SGB expects many unused registers to initialize to 0xFF.

**File:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MappedMemory.java`

```java
private final byte[] defaultMemory = new byte[0x10000];  // All zeros by default
```

**Issue:** Unmapped registers return 0x00 instead of 0xFF. SGB expects most unused I/O space to be 0xFF.

### 9. P1/JOYP Register (0xFF00)

**Issue:** The test expects P1 register to be 0xFF (no input selected, all bits high). The emulator does not appear to have a joypad controller mapped.

**Evidence:** No mapping for 0xFF00 in MappedMemory.

## Summary of Expected Failures

When running this test on the current emulator (configured for DMG-like behavior), the following mismatches will occur:

1. **0xFF00 (P1):** Expected 0xFF, likely got 0x00 (no joypad implementation)
2. **0xFF02 (SC):** Expected 0x7E, got 0x00
3. **0xFF05 (TIMA):** Expected 0xFF, got 0x00
4. **0xFF07 (TAC):** Expected 0xF8, got unknown (needs TacRegister analysis)
5. **0xFF0F (IF):** Expected 0xE1, got 0xE0
6. **0xFF10-0xFF26 (Audio):** Expected various values, got 0x00 (no APU)
7. **0xFF45 (LYC):** Expected 0xFF, got 0x00
8. **0xFF4C-0xFF7F (Unused):** Expected 0xFF, got 0x00

The test will fail very early (likely at 0xFF00 or 0xFF02) and report the first mismatch address.

## Recommendations

To make this test pass, the emulator would need to:

1. **Add Hardware Model Configuration:** Support different initial states for DMG, SGB, CGB, etc.
2. **Implement Proper Power-On State:** Initialize all registers to SGB-specific values
3. **Implement Missing Components:** Add APU registers and joypad register
4. **Fix Default Memory Initialization:** Consider initializing unused I/O space to 0xFF instead of 0x00 for SGB mode

**Note:** Since this test is specifically for SGB hardware and is expected to FAIL on DMG hardware (which the emulator appears to target), this may not be a priority fix unless SGB support is desired.
