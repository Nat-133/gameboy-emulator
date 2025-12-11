# Mooneye Test Analysis: boot_div-S

## Test Overview

The `boot_div-S` test validates the value and relative phase of the DIV register after the Super Game Boy (SGB/SGB2) boot ROM completes. This test is specifically designed for SGB/SGB2 hardware and expects to fail on DMG, MGB, CGB, AGB, and AGS systems.

The test verifies that:
1. The DIV register has the correct value immediately after boot ROM completion
2. The phase relationship between CPU instructions and DIV increments matches real SGB hardware
3. The timing is precise enough to detect single M-cycle differences

## What The Test Does

The test performs 6 sequential reads of the DIV register ($FF04) with carefully timed delays between each read to verify both the DIV value and its phase alignment:

### Detailed Step-by-Step Execution Flow

1. **Test Entry (after boot ROM completes)**
   - Control transfers to address $0150 (standard cartridge entry point)
   - Internal timer counter should be at ~0xD877 (DIV = 0xD8)

2. **First Read (33 NOPs + LDH)**
   - Execute 33 NOP instructions (132 T-cycles = 33 M-cycles)
   - Execute LDH A,($FF04) (12 T-cycles = 3 M-cycles)
   - Memory read occurs ~8 T-cycles into the LDH instruction
   - Expected counter value: 0xD903
   - Expected DIV value: 0xD9
   - Comment: "This read should happen immediately after DIV has incremented"
   - PUSH AF to save the value

3. **Second Read (57 NOPs + LDH)**
   - Execute 57 NOP instructions (228 T-cycles = 57 M-cycles)
   - Execute LDH A,($FF04) (12 T-cycles = 3 M-cycles)
   - Total delay from read 1: 256 T-cycles (exactly one DIV increment period)
   - Expected counter value: 0xDA03
   - Expected DIV value: 0xDA (incremented by 1)
   - Comment: "Should happen immediately after the next increment"
   - PUSH AF to save the value

4. **Third Read (56 NOPs + LDH)**
   - Execute 56 NOP instructions (224 T-cycles = 56 M-cycles)
   - Execute LDH A,($FF04) (12 T-cycles = 3 M-cycles)
   - Total delay from read 2: 252 T-cycles (4 T-cycles less than one DIV increment)
   - Expected counter value: 0xDAFF
   - Expected DIV value: 0xDA (SAME as read 2 - read happens before increment)
   - Comment: "Should happen immediately *before* the increment"
   - Phase has been shifted 4 T-cycles earlier
   - PUSH AF to save the value

5. **Fourth Read (57 NOPs + LDH)**
   - Execute 57 NOP instructions (228 T-cycles = 57 M-cycles)
   - Execute LDH A,($FF04) (12 T-cycles = 3 M-cycles)
   - Total delay from read 3: 256 T-cycles (one DIV increment period)
   - Expected counter value: 0xDBFF
   - Expected DIV value: 0xDB (incremented by 1 from read 3)
   - Comment: "Once again immediately *before* the increment"
   - Phase remains the same as read 3 (before increment)
   - PUSH AF to save the value

6. **Fifth Read (57 NOPs + LDH)**
   - Execute 57 NOP instructions (228 T-cycles = 57 M-cycles)
   - Execute LDH A,($FF04) (12 T-cycles = 3 M-cycles)
   - Total delay from read 4: 256 T-cycles (one DIV increment period)
   - Expected counter value: 0xDCFF
   - Expected DIV value: 0xDC (incremented by 1 from read 4)
   - Comment: "Same thing here..."
   - Phase still before increment
   - PUSH AF to save the value

7. **Sixth Read (58 NOPs + LDH)**
   - Execute 58 NOP instructions (232 T-cycles = 58 M-cycles)
   - Execute LDH A,($FF04) (12 T-cycles = 3 M-cycles)
   - Total delay from read 5: 260 T-cycles (4 T-cycles more than one DIV increment)
   - Expected counter value: 0xDE03
   - Expected DIV value: 0xDE (incremented by 2 from read 5)
   - Comment: "Alters the phase and the read should happen after the increment once again"
   - Phase has been shifted 4 T-cycles later (back to "after increment")
   - PUSH AF to save the value

8. **Validation**
   - POP the 6 saved AF values into registers B, C, D, E, H, L
   - Call setup_assertions to prepare the assertion framework
   - Assert each register has the expected value:
     - B = 0xD9 (read 1)
     - C = 0xDA (read 2)
     - D = 0xDA (read 3, same as read 2)
     - E = 0xDB (read 4)
     - H = 0xDC (read 5)
     - L = 0xDE (read 6)
   - Call quit_check_asserts to display results

### Instruction Timing Reference
- NOP: 4 T-cycles (1 M-cycle)
- LDH A,(n): 12 T-cycles (3 M-cycles), memory read occurs ~8 T-cycles into instruction
- PUSH AF: 16 T-cycles (4 M-cycles)
- POP AF: 12 T-cycles (3 M-cycles)

### DIV Register Timing
- DIV is the upper 8 bits of a 16-bit internal timer counter
- The internal counter increments every T-cycle (4.194304 MHz)
- DIV increments every 256 T-cycles (16.384 kHz)
- DIV "increments" when the counter crosses a 0xXX00 boundary

## What The Test Expects

### Expected Register Values
- **B (Read 1)**: 0xD9 - DIV value immediately after increment
- **C (Read 2)**: 0xDA - DIV incremented once from read 1
- **D (Read 3)**: 0xDA - Same as read 2 (read before next increment)
- **E (Read 4)**: 0xDB - DIV incremented once from read 3
- **H (Read 5)**: 0xDC - DIV incremented once from read 4
- **L (Read 6)**: 0xDE - DIV incremented twice from read 5

### Expected Internal Counter Values
- **Test start**: ~0xD877 (DIV displays as 0xD8)
- **Read 1**: 0xD903 (DIV = 0xD9)
- **Read 2**: 0xDA03 (DIV = 0xDA)
- **Read 3**: 0xDAFF (DIV = 0xDA)
- **Read 4**: 0xDBFF (DIV = 0xDB)
- **Read 5**: 0xDCFF (DIV = 0xDC)
- **Read 6**: 0xDE03 (DIV = 0xDE)

### Phase Alignment Pattern
The test verifies a specific phase pattern:
- Reads 1, 2, 6: "immediately after" increment (counter at 0xXX03)
- Reads 3, 4, 5: "immediately before" increment (counter at 0xXXFF)

This pattern demonstrates:
1. Initial phase can be maintained by using 256 T-cycle delays (57 NOPs + LDH)
2. Phase can be shifted earlier by using 252 T-cycle delays (56 NOPs + LDH)
3. Phase can be shifted later by using 260 T-cycle delays (58 NOPs + LDH)
4. Once phase is shifted, it remains shifted until another adjustment

## What The Test Is Testing

This test validates several critical aspects of Game Boy emulation:

### 1. Boot ROM Duration for SGB
The test verifies that the internal timer counter has advanced to the correct value by the time the boot ROM completes execution. The SGB boot ROM takes significantly longer to execute than other Game Boy models:

- **DMG (Game Boy)**: Boot ROM completes with DIV at ~0xAC
- **SGB (Super Game Boy)**: Boot ROM completes with DIV at ~0xD9
- **Difference**: 0x2D (45 decimal) DIV increments = 11,520 T-cycles = 2,880 M-cycles

This 2.75ms difference reflects the additional time the SGB boot ROM takes to:
- Initialize Super Game Boy-specific hardware
- Perform additional checksums and validation
- Calculate timing based on ROM header bytes (including global checksum)

The test uses `.define CART_NO_GLOBAL_CHECKSUM` to use a fixed (invalid) checksum, avoiding flakiness where different ROM content would cause different boot ROM durations.

### 2. DIV Register Precision
The test validates that:
- DIV increments at exactly the correct rate (every 256 T-cycles)
- DIV reads are cycle-accurate (reading the correct upper 8 bits of the internal counter)
- DIV register is properly implemented as a view into the internal timer counter
- Writing to DIV ($FF04) properly resets the internal counter to 0

### 3. Timer Phase Alignment
The test checks that the emulator maintains correct phase relationships between:
- CPU instruction execution
- Internal timer counter increments
- DIV register boundary crossings

The ability to shift phase by ±4 T-cycles and detect "before" vs "after" increment timing demonstrates that the emulator must be cycle-accurate at the T-cycle level, not just the M-cycle level.

### 4. Post-Boot State Initialization
The test assumes:
- Emulator either runs the SGB boot ROM OR fast-forwards to the correct post-boot state
- Internal timer counter continues running during boot ROM execution
- DIV starts at 0 when the system powers on
- No spurious timer resets occur during boot

### 5. SGB-Specific Behavior
This test specifically validates SGB/SGB2 hardware behavior, which differs from other Game Boy models in:
- Boot ROM duration (significantly longer than DMG/MGB)
- Boot ROM timing dependencies on ROM header data
- Initial hardware state after boot

## Potential Failure Reasons

### 1. Incorrect Initial Timer Counter Value

**Current Emulator State:**
- `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/CoreModule.java` line 55:
  ```java
  return new InternalTimerCounter(0xAAC8);
  ```
- This sets the initial counter to 0xAAC8, which gives DIV = 0xAA
- Comment says: "Post-boot state for DMG/MGB"

**Problem:**
- The current value (0xAAC8) is for DMG/MGB, not SGB
- For SGB, the counter should be ~0xD877 (DIV = 0xD8) at test entry
- This is a difference of 0x2DAF (11,695) T-cycles

**Why This Matters:**
- The test expects DIV to be 0xD9 after the first sequence of NOPs
- With counter starting at 0xAAC8, DIV would be around 0xAC after the first read
- The test would see 0xAC instead of 0xD9 and fail all assertions

**Solution Required:**
The emulator needs to:
1. Detect that it's running in SGB mode (perhaps based on boot ROM or configuration)
2. Initialize the internal timer counter to 0xD877 (or calculate based on SGB boot ROM duration)
3. Ensure proper phase alignment (counter at 0xXX77 gives the correct phase)

### 2. Boot ROM Not Executed or Incorrect Boot ROM

**Observation:**
- The test assumes execution starts after the boot ROM completes
- The boot ROM duration directly affects the final DIV value

**Potential Issues:**
1. **No Boot ROM**: If the emulator skips the boot ROM entirely, it must still set the correct post-boot timer state
2. **Wrong Boot ROM**: If a DMG boot ROM is used instead of SGB boot ROM, timing will be wrong
3. **Fast-Forward Bug**: If the emulator "fast-forwards" through boot but doesn't advance the timer correctly
4. **Boot ROM Duration**: If the SGB boot ROM emulation takes the wrong amount of time

**Current Emulator Behavior:**
- Looking at `MemoryInitializer.java`, the emulator can load a boot ROM
- The hardcoded value in `CoreModule.java` suggests the emulator may be skipping boot ROM execution
- The comment "Post-boot state for DMG/MGB" confirms this is meant to simulate post-boot state

**Solution Required:**
- Either run the actual SGB boot ROM with correct timing
- OR set the initial counter value differently based on the target system (DMG vs SGB vs CGB)
- The current single hardcoded value cannot support multiple system types

### 3. Phase Alignment Issues

**Expected Phase:**
- Counter should be at 0xXX03 for "immediately after" increment reads
- Counter should be at 0xXXFF for "immediately before" increment reads

**Current Emulator:**
- Initial counter 0xAAC8 has phase 0xC8 (200 decimal)
- After 140 T-cycles (33 NOPs + 8 T into LDH): phase becomes (0xC8 + 140) & 0xFF = 0x54 (84 decimal)
- This is roughly mid-way between increments, not "immediately after"

**Problem:**
- The phase component (lower 8 bits) is wrong
- Should be 0x77 at test start to reach 0x03 after 140 T-cycles: (0x77 + 140) & 0xFF = 0x03
- Current 0xC8 phase will never align with the expected pattern

**Why This Matters:**
- Even if the DIV upper byte is corrected (0xAA -> 0xD8), the phase is still wrong
- The test will see different DIV values at the read points
- The "before" vs "after" pattern will not match expectations

### 4. Timer Implementation Issues

**Potential Implementation Bugs:**

1. **DIV Read Timing**:
   - `DividerRegister.read()` at line 13: `return (byte) ((internalCounter.getValue() >> 8) & 0xFF);`
   - This looks correct - returns upper 8 bits
   - But timing of when the read happens during instruction execution matters
   - Read should happen during the memory access cycle, not at instruction start/end

2. **Timer Counter Increment Rate**:
   - `InternalTimerCounter.tCycle()` at line 15: `counter = (counter + 1) & 0xFFFF;`
   - This looks correct - increments every T-cycle
   - But needs to be called exactly 4 times per M-cycle
   - Must be synchronized with CPU instruction execution

3. **DIV Write (Reset) Behavior**:
   - `DividerRegister.write()` at line 18-20: `internalCounter.reset();`
   - This should reset counter to 0 regardless of the value written
   - The reset behavior looks correct

4. **Timer/CPU Synchronization**:
   - `Timer.mCycle()` at line 70-78 calls `internalCounter.tCycle()` 4 times
   - This looks correct for maintaining T-cycle accuracy
   - But must be called exactly once per M-cycle of CPU execution
   - Any drift will cause phase misalignment

### 5. Configuration/System Detection

**Problem:**
- The emulator currently uses a single hardcoded timer value for all systems
- It needs to differentiate between DMG, MGB, SGB, SGB2, CGB, AGB, AGS
- Each system has different boot ROM duration and thus different post-boot timer state

**Required:**
The emulator needs:
1. System type detection or configuration
2. Different initial timer values per system type:
   - DMG: ~0xAC DC (requires verification of exact phase)
   - MGB: ~0xAC DC (same as DMG)
   - SGB: ~0xD877 (as calculated above)
   - SGB2: ~0xD877 (same as SGB)
   - CGB: Different value (not tested here)
   - Others: Different values

3. Or implement actual boot ROM execution with correct timing per system

### 6. Instruction Timing Accuracy

**Requirement:**
- The test depends on precise T-cycle timing
- Each instruction must take exactly the right number of T-cycles
- The memory read within LDH must happen at the correct T-cycle

**Potential Issues:**
1. If NOP takes wrong number of cycles (should be 4 T-cycles = 1 M-cycle)
2. If LDH takes wrong number of cycles (should be 12 T-cycles = 3 M-cycles)
3. If PUSH takes wrong number of cycles (should be 16 T-cycles = 4 M-cycles)
4. If memory reads happen at wrong time within instruction execution
5. If timer updates don't happen at exact T-cycle boundaries

**Validation Needed:**
- Check that `Opcodes.json.gz` has correct cycle counts
- Verify CPU execution loop advances timer correctly
- Ensure memory accesses are synchronized with timer increments

### Summary of Most Likely Failures

1. **Wrong Initial Timer Value** (99% likely): Counter is 0xAAC8 instead of 0xD877
   - Test will see DIV ~0xAC instead of 0xD9 on first read
   - All subsequent reads will be offset by the same amount

2. **Wrong Phase Alignment** (99% likely): Phase is 0xC8 instead of 0x77
   - Even if DIV upper byte is corrected, phase pattern won't match
   - "Before" vs "after" timing will be wrong

3. **No SGB Mode Support** (90% likely): Emulator doesn't distinguish SGB from DMG
   - Same timer initialization used for all systems
   - Would need system-specific configuration

4. **Boot ROM Not Executed** (70% likely): Emulator skips boot ROM
   - This is actually OK if post-boot state is set correctly
   - But requires different states per system type

The test will definitely fail with the current implementation because the initial timer value is for DMG/MGB (0xAAC8) rather than SGB (0xD877). Both the upper byte and phase alignment are incorrect for SGB.
