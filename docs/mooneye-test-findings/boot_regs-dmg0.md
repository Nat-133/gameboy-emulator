# Mooneye Test Analysis: boot_regs-dmg0

## Test Overview

The `boot_regs-dmg0` test validates the initial CPU register values after the boot ROM completes execution. This test is specifically designed for the original DMG-0 (DMG-CPU) hardware revision and expects different register values compared to later revisions (DMG-A, DMG-B, DMG-C).

**Expected Pass/Fail:**
- Pass: DMG 0 (original Game Boy hardware)
- Fail: DMG ABC, MGB, SGB, SGB2, CGB, AGB, AGS (all later revisions)

## What The Test Does

### Step-by-Step Execution Flow:

1. **Save Initial Stack Pointer** (lines 33-34)
   - Saves the current SP value to HRAM location `sp_save`
   - Sets SP to 0xFFFE temporarily for test operations

2. **Verify Stack Pointer** (lines 36-44)
   - Pushes AF to the stack (to preserve it)
   - Reads back the saved SP value from HRAM
   - Compares low byte: expects 0xFE (low byte of 0xFFFE)
   - Compares high byte: expects 0xFF (high byte of 0xFFFE)
   - If SP doesn't match 0xFFFE, jumps to `invalid_sp` failure handler
   - Pops AF to restore registers

3. **Setup Assertion Framework** (line 50)
   - `setup_assertions` macro:
     - Disables interrupts (di)
     - Saves all registers (AF, BC, DE, HL) to HRAM at `hram.regs_save`
     - Resets SP to DEFAULT_SP (0xE000)
     - Clears assertion flags and expected values

4. **Configure Expected Register Values** (lines 51-58)
   - Sets expected values for each register using assert_X macros
   - Each macro stores the expected value in HRAM and sets a flag bit
   - **DMG-0 specific expected values:**
     - A = 0x01
     - F = 0x00 (ALL FLAGS CLEAR)
     - B = 0xFF
     - C = 0x13
     - D = 0x00
     - E = 0xC1
     - H = 0x84
     - L = 0x03

5. **Verify Assertions and Report** (line 59)
   - `quit_check_asserts` calls the `check_asserts_cb` callback
   - Compares saved register values against expected values
   - Prints register dump showing actual vs expected
   - Returns success (d=0x00) if all match, failure (d=0x42) if any mismatch

## What The Test Expects

### Expected Register Values (DMG-0):

| Register | Expected Value | Notes |
|----------|---------------|-------|
| **SP** | 0xFFFE | Stack pointer at top of memory |
| **A** | 0x01 | Accumulator = 1 |
| **F** | 0x00 | All flags clear (Z=0, N=0, H=0, C=0) |
| **B** | 0xFF | B register = 255 |
| **C** | 0x13 | C register = 19 |
| **D** | 0x00 | D register = 0 |
| **E** | 0xC1 | E register = 193 |
| **H** | 0x84 | H register = 132 |
| **L** | 0x03 | L register = 3 |

### Composite Register Values:
- **AF** = 0x0100 (A=0x01, F=0x00)
- **BC** = 0xFF13 (B=0xFF, C=0x13)
- **DE** = 0x00C1 (D=0x00, E=0xC1)
- **HL** = 0x8403 (H=0x84, L=0x03)
- **SP** = 0xFFFE
- **PC** = 0x0100 (execution starts here after boot ROM)

## What The Test Is Testing

This test validates:

1. **Boot ROM Completion**: That the boot ROM executed correctly and left registers in the expected state
2. **Hardware Revision Detection**: DMG-0 hardware leaves registers in a unique state different from later revisions
3. **Initial CPU State**: The exact register values after boot ROM disables itself (writes to BOOT register at 0xFF50)

The boot ROM on real hardware performs several operations:
- Scrolls the Nintendo logo
- Plays the startup sound
- Initializes hardware registers
- Sets up initial CPU register values
- **These register values differ between hardware revisions**

## Potential Failure Reasons

### Current Emulator Configuration (CpuModule.java, lines 31-40):

```java
return new CpuRegisters(
    (short) 0x01B0,  // af - A=0x01, F=0xB0 (Z=1, N=0, H=1, C=1)
    (short) 0x0013,  // bc - B=0x00, C=0x13
    (short) 0x00D8,  // de - D=0x00, E=0xD8
    (short) 0x014D,  // hl - H=0x01, L=0x4D
    (short) 0xFFFE,  // sp - Stack pointer at 0xFFFE
    (short) 0x0100,  // pc - starts at 0x100 after boot ROM
    (byte) 0x00,     // instructionRegister
    false            // ime - disabled after boot ROM
);
```

### Identified Mismatches:

The emulator is currently configured for **DMG-ABC** hardware, NOT DMG-0:

| Register | DMG-0 Expected | Emulator Actual | Match? |
|----------|---------------|-----------------|--------|
| **AF** | 0x0100 | 0x01B0 | NO - F register wrong |
| **BC** | 0xFF13 | 0x0013 | NO - B register wrong |
| **DE** | 0x00C1 | 0x00D8 | NO - E register wrong |
| **HL** | 0x8403 | 0x014D | NO - Both H and L wrong |
| **SP** | 0xFFFE | 0xFFFE | YES |
| **PC** | 0x0100 | 0x0100 | YES |

### Specific Mismatches:

1. **F Register (Flags):**
   - Expected: 0x00 (all flags clear)
   - Actual: 0xB0 (Z=1, H=1, C=1)
   - **This is the DMG-ABC value, not DMG-0**

2. **B Register:**
   - Expected: 0xFF
   - Actual: 0x00
   - **This is the DMG-ABC value, not DMG-0**

3. **E Register:**
   - Expected: 0xC1
   - Actual: 0xD8
   - **This is the DMG-ABC value, not DMG-0**

4. **H Register:**
   - Expected: 0x84
   - Actual: 0x01
   - **This is the DMG-ABC value, not DMG-0**

5. **L Register:**
   - Expected: 0x03
   - Actual: 0x4D
   - **This is the DMG-ABC value, not DMG-0**

### Root Cause:

The emulator's `CpuModule.provideCpuRegisters()` method initializes registers to match the **DMG-ABC (later revision)** hardware specification, as confirmed by comparing with the `boot_regs-dmgABC.s` test file which expects:
- AF = 0x01B0
- BC = 0x0013
- DE = 0x00D8
- HL = 0x014D

To pass the DMG-0 test, the emulator would need to be reconfigured to use DMG-0 initial values instead.

### Why This Matters:

Different Game Boy hardware revisions leave different register values after boot. While most commercial games don't rely on these specific values (they initialize their own state), some homebrew and test ROMs use these values to detect hardware revision. The emulator currently emulates DMG-ABC behavior, which is actually more common and compatible with most software.

### Recommended Action:

**Do NOT change the emulator's register initialization.** The emulator is correctly configured for DMG-ABC, which is:
1. More common hardware revision
2. What most commercial games expect
3. The standard reference implementation

The boot_regs-dmg0 test is specifically designed to FAIL on non-DMG-0 hardware, and the emulator is correctly emulating DMG-ABC behavior. This test failure is **expected and correct** for a DMG-ABC emulator.

If DMG-0 emulation is needed, it should be implemented as a configurable option, not as the default behavior.
