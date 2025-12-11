# Mooneye Test Analysis: boot_div-dmg0

## Test Overview

The `boot_div-dmg0` test validates the DIV (Divider Register) value and relative phase after the Game Boy boot ROM completes execution. This test is specific to DMG model 0 hardware and is expected to fail on all other Game Boy models (DMG ABC, MGB, SGB, SGB2, CGB, AGB, AGS).

The test checks that:
1. The DIV register has a specific initial value after boot ROM completion
2. The DIV register increments at precise intervals
3. The phase relationship between instruction execution and DIV increments is correct

## What The Test Does

The test executes a carefully timed sequence of operations to sample the DIV register at specific moments:

### Step-by-Step Execution Flow

1. **Initial Setup**: After boot ROM completes (execution starts at $0150), the test begins

2. **First Sample (45 NOPs)**:
   - Executes 45 NOP instructions
   - Reads DIV register: `ldh a, (<DIV)`
   - Pushes value to stack: `push af`
   - Comment: "This read should happen immediately after DIV has incremented"

3. **Second Sample (57 NOPs)**:
   - Executes 57 NOP instructions
   - Reads DIV register: `ldh a, (<DIV)`
   - Pushes value to stack: `push af`
   - Comment: "With 57 NOPs here, the next read should happen immediately after the next increment. So, the relative phase between the read and the increment remains the same"

4. **Third Sample (56 NOPs)**:
   - Executes 56 NOP instructions (one less than before)
   - Reads DIV register: `ldh a, (<DIV)`
   - Pushes value to stack: `push af`
   - Comment: "This time we have only 56 NOPs, so the next read should happen immediately *before* the increment because we're altering the relative phase and reading one M-cycle earlier"

5. **Fourth Sample (57 NOPs)**:
   - Executes 57 NOP instructions
   - Reads DIV register: `ldh a, (<DIV)`
   - Pushes value to stack: `push af`
   - Comment: "Since we're back to 57 NOPs, the next read should happen once again immediately *before* the increment. Phase is not changed here, but the change in the earlier step remains"

6. **Fifth Sample (57 NOPs)**:
   - Executes 57 NOP instructions
   - Reads DIV register: `ldh a, (<DIV)`
   - Pushes value to stack: `push af`
   - Comment: "Same thing here..."

7. **Sixth Sample (58 NOPs)**:
   - Executes 58 NOP instructions (one more than before)
   - Reads DIV register: `ldh a, (<DIV)`
   - Pushes value to stack: `push af`
   - Comment: "This time we have 58 NOPs, which alters the phase and the read should happen after the increment once again"

8. **Extract Values from Stack**:
   - Pops values from stack in reverse order into registers B, C, D, E, H, L
   - The order of popping means:
     - B = first sample
     - C = second sample
     - D = third sample
     - E = fourth sample
     - H = fifth sample
     - L = sixth sample

9. **Setup Assertions and Check**:
   - Calls `setup_assertions` to prepare assertion framework
   - Asserts specific expected values for each register
   - Calls `quit_check_asserts` to validate and display results

## What The Test Expects

The test expects these exact DIV values at each checkpoint:

- **Register B** (1st sample after 45 NOPs): `$19` (25 decimal)
- **Register C** (2nd sample after 57 NOPs): `$1A` (26 decimal)
- **Register D** (3rd sample after 56 NOPs): `$1A` (26 decimal)
- **Register E** (4th sample after 57 NOPs): `$1B` (27 decimal)
- **Register H** (5th sample after 57 NOPs): `$1C` (28 decimal)
- **Register L** (6th sample after 58 NOPs): `$1E` (30 decimal)

### Timing Analysis

DIV increments every 256 T-cycles (64 M-cycles). Each instruction takes:
- NOP: 1 M-cycle (4 T-cycles)
- LDH A, (n): 3 M-cycles (12 T-cycles)
- PUSH AF: 4 M-cycles (16 T-cycles)

Between samples:
- Sample 1: 45 NOPs + LDH + PUSH = 45 + 3 + 4 = 52 M-cycles
- Sample 2: 57 NOPs + LDH + PUSH = 57 + 3 + 4 = 64 M-cycles (exactly one DIV increment)
- Sample 3: 56 NOPs + LDH + PUSH = 56 + 3 + 4 = 63 M-cycles (one less than DIV increment)
- Sample 4: 57 NOPs + LDH + PUSH = 57 + 3 + 4 = 64 M-cycles
- Sample 5: 57 NOPs + LDH + PUSH = 57 + 3 + 4 = 64 M-cycles
- Sample 6: 58 NOPs + LDH + PUSH = 58 + 3 + 4 = 65 M-cycles (one more than DIV increment)

### Phase Relationship Observations

- Sample 1 to 2: C = B + 1 (DIV incremented once, phase maintained)
- Sample 2 to 3: D = C (DIV did NOT increment due to phase shift - reading 1 M-cycle earlier)
- Sample 3 to 4: E = D + 1 (DIV incremented, new phase maintained)
- Sample 4 to 5: H = E + 1 (DIV incremented, phase maintained)
- Sample 5 to 6: L = H + 2 (DIV incremented twice due to 65 M-cycle delay crossing two boundaries)

## What The Test Is Testing

This test validates:

1. **Boot ROM Timing**: The exact number of cycles executed by the DMG-0 boot ROM, which determines the initial DIV value when user code starts at $0150

2. **DIV Register Behavior**:
   - DIV increments every 256 T-cycles (64 M-cycles)
   - DIV is the upper 8 bits of a 16-bit internal counter that increments every T-cycle
   - The internal counter continues counting during and after boot ROM execution

3. **Timing Precision**:
   - Tests cycle-accurate instruction execution
   - Tests precise DIV increment timing
   - Tests phase alignment between instruction execution and DIV increments

4. **Model-Specific Boot Behavior**:
   - DMG-0 has a specific boot ROM duration and initial state
   - Other Game Boy models have different boot ROM timings, hence different initial DIV values

## Potential Failure Reasons

Based on the emulator code analysis, here are the potential reasons why this test might fail:

### 1. Incorrect Initial Internal Counter Value

**File**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/CoreModule.java`

**Current Implementation**:
```java
@Provides
@Singleton
InternalTimerCounter provideInternalTimerCounter() {
    // Post-boot state for DMG/MGB (DIV = $AB, aligned for boot_sclk_align test)
    return new InternalTimerCounter(0xAAC8);
}
```

**Issue**: The comment indicates this value ($AAC8) is set for "boot_sclk_align test", but `boot_div-dmg0` requires a different initial value. The initial value needs to be calculated based on:
- The exact number of cycles the DMG-0 boot ROM executes
- The starting state of the internal counter at power-on

With `0xAAC8` as the initial value, DIV would be `0xAA` (170 decimal). After 45 NOPs (45 M-cycles = 180 T-cycles), the internal counter would be approximately `0xAAC8 + 180 = 0xAB7C`, giving DIV = `0xAB` (171 decimal), not `0x19` (25 decimal) as expected.

**Expected Initial Value Calculation**:
The test expects DIV = $19 after 45 NOPs from $0150. Working backwards:
- After 45 NOPs: internal counter upper byte should be $19
- Before 45 NOPs: internal counter should be approximately `(0x19 << 8) - 180 = 0x1900 - 0xB4 = 0x184C`
- This means at the start of user code ($0150), the internal counter should be around `0x184C`

However, we need to account for the exact timing of the LDH instruction too. The test says "read should happen immediately after DIV has incremented", suggesting the phase is important.

### 2. Boot ROM Not Simulated

The emulator appears to skip boot ROM simulation entirely. The test file is named `boot_div-dmg0.s`, indicating it's testing behavior *after* boot ROM completes. The current implementation sets an initial counter value but doesn't actually execute a boot ROM.

**Missing Functionality**:
- No boot ROM execution tracked in the codebase
- The initial counter value is a hardcoded approximation
- No model-specific boot ROM timing differences

### 3. Incorrect DIV Increment Frequency

**File**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/DividerRegister.java`

**Current Implementation**:
```java
@Override
public byte read() {
    return (byte) ((internalCounter.getValue() >> 8) & 0xFF);
}
```

This correctly implements DIV as the upper 8 bits of the 16-bit counter, which means DIV increments every 256 T-cycles. This appears correct.

### 4. Instruction Timing Issues

If CPU instructions don't execute with cycle-accurate timing, the test will fail. The test is extremely timing-sensitive:
- A difference of just 1 M-cycle changes which side of a DIV increment the read occurs on
- Sample 3 uses 56 NOPs specifically to shift phase by 1 M-cycle

**Potential Issues**:
- NOP might not take exactly 1 M-cycle
- LDH A, (n) might not take exactly 3 M-cycles
- PUSH AF might not take exactly 4 M-cycles
- Clock synchronization issues between CPU and timer

### 5. Memory-Mapped I/O Timing

**File**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/DividerRegister.java`

When reading DIV via `ldh a, (<DIV)`, the read happens at a specific point in the instruction's execution. If the memory read doesn't happen at the correct cycle within the 3 M-cycle instruction, the test could fail.

### 6. DMG-0 vs Other Models

The test explicitly states it only passes on DMG-0 hardware. The emulator doesn't appear to have model-specific boot sequences. The current hardcoded value might be for a different model (the comment mentions DMG/MGB).

### Summary of Primary Issues

1. **Wrong Initial Counter Value**: The hardcoded `0xAAC8` value gives DIV = `0xAA`, but the test expects a sequence starting with DIV = `0x19`. The emulator needs the correct initial value for DMG-0 specifically.

2. **No Boot ROM Simulation**: The emulator should either:
   - Execute the actual DMG-0 boot ROM and let the counter run naturally
   - Calculate the exact counter value at boot ROM completion for DMG-0

3. **Cycle-Accurate Execution Required**: Any timing inaccuracy in instruction execution will cause this test to fail, as it's testing phase relationships at the M-cycle level.

The most critical fix needed is determining and setting the correct initial internal counter value for DMG-0 hardware at address $0150 (where user code starts after boot ROM).
