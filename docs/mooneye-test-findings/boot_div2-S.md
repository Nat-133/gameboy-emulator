# Mooneye Test Analysis: boot_div2-S

## Test Overview

The `boot_div2-S` test validates the DIV (Divider Register at address 0xFF04) register value and timing phase after the boot ROM completes on Super Game Boy (SGB/SGB2) hardware. This is a variant of `boot_div-S` that uses a different global checksum value to expose hard-coded boot ROM durations, as the SGB boot ROM duration varies based on ROM header bytes including the global checksum.

**Target Hardware**: SGB, SGB2 (fails on DMG, MGB, CGB, AGB, AGS)

**Source File**: `/Users/nathaniel.manley/vcs/personal/mooneye-test-suite/acceptance/boot_div2-S.s`

## What The Test Does

The test executes immediately after the boot ROM finishes (starting at address 0x0150). It performs a series of timed DIV register reads with precise NOP delays to verify both the absolute DIV value and the relative timing phase between reads and DIV increments.

### Step-by-Step Execution Flow

1. **Initial Setup** (37 NOPs)
   - Executes 37 NOP instructions to align timing
   - Each NOP takes 1 M-cycle (4 T-cycles)

2. **First DIV Read** (after 37 NOPs)
   - `ldh a, (<DIV)` - Read DIV into register A (3 M-cycles)
   - `push af` - Push A register to stack (4 M-cycles)
   - Expected to read immediately AFTER DIV increments
   - Expected value: 0xD9

3. **Second DIV Read** (after 57 NOPs)
   - 57 NOPs = 57 M-cycles
   - `ldh a, (<DIV)` and `push af`
   - Should maintain the same phase (read AFTER increment)
   - Expected value: 0xDA

4. **Third DIV Read** (after 56 NOPs)
   - 56 NOPs = 56 M-cycles (one less than before)
   - This shifts the phase by 1 M-cycle earlier
   - Should now read immediately BEFORE DIV increments
   - Expected value: 0xDA (same as previous, hasn't incremented yet)

5. **Fourth DIV Read** (after 57 NOPs)
   - Back to 57 NOPs, but phase shift remains
   - Still reads BEFORE increment
   - Expected value: 0xDB

6. **Fifth DIV Read** (after 57 NOPs)
   - Another 57 NOPs, phase unchanged
   - Still reads BEFORE increment
   - Expected value: 0xDC

7. **Sixth DIV Read** (after 58 NOPs)
   - 58 NOPs = one extra M-cycle
   - This shifts phase back to reading AFTER increment
   - Expected value: 0xDE (skips 0xDD due to phase shift)

8. **Register Setup and Assertions**
   - Pop all values from stack into registers (B, C, D, E, H, L)
   - Call `setup_assertions` to save register state
   - Assert expected values for each register
   - Call `quit_check_asserts` to verify and report results

9. **Checksum Bytes**
   - `.org $014e` places data at ROM address 0x014E
   - `.dw $a796` - Global checksum bytes set to 0xA796
   - This differs from `boot_div-S` which uses 0x1234
   - The different checksum causes different SGB boot ROM duration

## What The Test Expects

The test expects the following register values after all reads:

- **B register**: 0xD9 (first read)
- **C register**: 0xDA (second read)
- **D register**: 0xDA (third read - same as second due to phase shift)
- **E register**: 0xDB (fourth read)
- **H register**: 0xDC (fifth read)
- **L register**: 0xDE (sixth read - skips 0xDD due to phase shift)

### Key Timing Characteristics

1. **DIV Increment Rate**: DIV increments every 64 M-cycles (256 T-cycles)
   - The DIV register is the upper 8 bits of a 16-bit internal counter
   - Internal counter increments every T-cycle
   - When bits 0-7 overflow, DIV increments

2. **Phase Relationship**: The test validates that:
   - After 37 NOPs + 3 M-cycles (ldh), the read happens right AFTER an increment
   - 57 NOPs + 3 M-cycles maintains this "after" phase
   - 56 NOPs + 3 M-cycles shifts to "before" phase (1 M-cycle earlier)
   - 58 NOPs + 3 M-cycles shifts back to "after" phase (1 M-cycle later)

3. **Initial DIV Value**: Starting from boot ROM completion, the first read expects 0xD9
   - This implies a specific boot ROM duration for SGB with this checksum

## What The Test Is Testing

### Primary Test Target: SGB Boot ROM Duration with Checksum Variance

The test validates:

1. **Boot ROM Duration**: The SGB boot ROM must complete at a specific cycle count that results in DIV = 0xD9 after the initial 37 NOP delay

2. **Checksum-Dependent Timing**: SGB/SGB2 boot ROM duration varies based on the ROM header global checksum. This test uses checksum 0xA796 (vs 0x1234 in boot_div-S) to ensure emulators don't use a hard-coded duration

3. **DIV Timing Precision**: The internal 16-bit counter and DIV register must:
   - Increment at exactly 256 T-cycles (64 M-cycles)
   - Maintain correct phase relationship between instruction execution and increment
   - Properly expose the upper 8 bits as the DIV register value

4. **Instruction Timing Accuracy**: Tests that:
   - NOP executes in exactly 1 M-cycle
   - `ldh a, (<addr)` executes in exactly 3 M-cycles
   - `push af` executes in exactly 4 M-cycles
   - Phase shifts of 1 M-cycle are properly reflected

## Potential Failure Reasons

### 1. Incorrect Initial DIV Value (Most Likely)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/CoreModule.java`

```java
@Provides
@Singleton
InternalTimerCounter provideInternalTimerCounter() {
    // Post-boot state for DMG/MGB (DIV = $AB, aligned for boot_sclk_align test)
    return new InternalTimerCounter(0xAAC8);
}
```

**Problem**: The emulator initializes the internal timer counter to 0xAAC8, which gives DIV = 0xAA (upper 8 bits). This is documented as "Post-boot state for DMG/MGB", but this test is for SGB/SGB2 which has different boot ROM timing.

**Expected Behavior**: For SGB with checksum 0xA796, after the boot ROM completes and the test executes 37 NOPs + 3 M-cycles (ldh instruction), the DIV register should read 0xD9.

**Calculation**:
- After boot ROM, if DIV should be 0xD9 after 40 M-cycles (37 NOPs + 3 for ldh)
- This means at boot ROM completion, DIV was approximately 0xD9 - 1 = 0xD8 or close to it
- The internal counter would need to be around 0xD800 - 0xD900 range
- Current value 0xAAC8 (DIV = 0xAA) is far too low

**Why This Fails**: When the test runs with initial DIV = 0xAA, after 40 M-cycles DIV will still be around 0xAA, not 0xD9. The timing is completely wrong for SGB boot ROM duration.

### 2. SGB Boot ROM Not Implemented

The emulator doesn't appear to have any code that distinguishes between DMG and SGB boot ROM durations. The test comment explicitly states that SGB boot ROM duration depends on ROM header bytes including the checksum, but the emulator:

1. Has a hard-coded initial timer value
2. Doesn't parse or use ROM checksum for boot duration
3. Doesn't implement SGB boot ROM behavior at all

**Expected Behavior**: The emulator should either:
- Actually execute an SGB boot ROM (which would naturally produce the correct DIV value)
- Calculate the expected post-boot DIV value based on ROM checksum and target hardware
- Skip SGB-specific tests if not implementing SGB support

### 3. Boot ROM Execution vs. Skipping

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryInitializer.java`

```java
public List<MemoryDump> createMemoryDumps(String bootRomPath, String gameRomPath) throws IOException {
    List<MemoryDump> dumps = new ArrayList<>();

    byte[] gameRom = romLoader.loadRom(gameRomPath, 0x8000);
    dumps.add(MemoryDump.fromZero(gameRom));

    if (bootRomPath != null) {
        byte[] bootRom = romLoader.loadRom(bootRomPath, 0x100);
        dumps.add(MemoryDump.fromZero(bootRom));
    }

    return dumps;
}
```

**Problem**: The emulator loads the boot ROM into memory, but it's unclear if it actually executes it. The CPU initialization suggests boot ROM is skipped:

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/CpuModule.java`

```java
(short) 0x0100,  // pc - starts at 0x100 after boot ROM
```

The PC starts at 0x0100, which is the post-boot state. This means the boot ROM is likely not being executed, just the post-boot state is assumed.

**Expected Behavior**: For accurate timing, either:
1. Execute the actual boot ROM and let the timer run naturally
2. Calculate the exact post-boot timer state for the specific hardware and ROM being tested

### 4. DIV Timing Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/DividerRegister.java`

```java
public byte read() {
    return (byte) ((internalCounter.getValue() >> 8) & 0xFF);
}
```

The DIV read implementation looks correct - it returns the upper 8 bits of the 16-bit internal counter.

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/InternalTimerCounter.java`

```java
public void tCycle() {
    int oldValue = counter;
    counter = (counter + 1) & 0xFFFF;
    // ...
}
```

The counter increments by 1 each T-cycle, which is correct. DIV should increment every 256 T-cycles (when lower 8 bits overflow).

**Potential Issue**: The test expects very precise timing. Any misalignment in:
- Instruction cycle counts
- Memory access timing
- Boot ROM duration
- Initial counter value

...will cause the phase relationship and absolute values to be wrong.

### 5. Lack of SGB-Specific Support

The test explicitly targets SGB/SGB2 hardware and expects it to fail on DMG/MGB/CGB. The emulator appears to only implement DMG/MGB behavior based on the comment "Post-boot state for DMG/MGB".

**Recommendation**: If SGB support is not planned, this test should be marked as expected to fail or skipped. If SGB support is planned, the emulator needs:
1. SGB boot ROM
2. Checksum-dependent boot duration calculation
3. Correct post-boot timer values for SGB

## Summary

The test will fail because the emulator is initialized with a DIV value appropriate for DMG/MGB (0xAA), but this test expects SGB timing (DIV starting around 0xD9). The SGB boot ROM has a significantly different duration than DMG, and this duration varies based on the ROM's global checksum. The emulator does not implement SGB-specific boot behavior or timing, making it impossible for this test to pass without either:

1. Implementing actual SGB boot ROM execution
2. Calculating and using the correct post-boot timer state for SGB with the specific ROM checksum
3. Adding a hardware mode selection that adjusts boot timing accordingly
