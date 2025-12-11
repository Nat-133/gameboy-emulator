# halt_ime1_timing2-GS Test Analysis

## Test Overview

This test verifies that HALT with IME=1 (Interrupt Master Enable flag set) services interrupts with **exactly the same timing** as if the CPU were executing NOP instructions instead. The test ensures HALT doesn't add any delay when interrupting in the IME=1 case.

**Verified to pass on**: DMG, MGB, SGB, SGB2
**Verified to fail on**: CGB, AGB, AGS

## What The Test Does

The test performs 4 rounds of timing measurements, comparing HALT vs NOPs to ensure identical timing:

### Initial Setup
1. Disable interrupts (`di`)
2. Wait for scanline LY=10
3. Enable VBLANK interrupt in IE register
4. Clear IF register

### Round 1: HALT with long delay (Expected: $11)
1. Set interrupt handler target to `test_round1`
2. Enable interrupts (`ei`)
3. Execute `HALT` - waits for VBLANK interrupt
4. Interrupt handler begins:
   - Set next handler target to `finish_round1`
   - Clear IF register
   - Enable interrupts (`ei`)
   - Execute 12 NOPs
   - Reset DIV register to 0 (`xor a; ldh (<DIV), a`)
   - Execute delay loop: `delay_long_time 2502` = 2502 * 7 + 5 = 17519 cycles
   - Execute 7 more NOPs
   - Should NOT reach `jp fail_round1` if timing correct
5. Next VBLANK interrupt arrives at `finish_round1`:
   - Read DIV register value → save to D register
   - Expected value: $11 (17 decimal)

### Round 2: HALT with different delay (Expected: $12)
1. Set handler to `test_round2`, then `finish_round2`
2. Similar structure but:
   - 11 NOPs instead of 12
   - Same 2502 delay loop
   - 8 NOPs instead of 7
3. Read DIV → save to E register
4. Expected value: $12 (18 decimal)

### Round 3: HALT immediately (Expected: $11)
1. Set handler to `test_round3`, then `finish_round3`
2. Execute sequence:
   - 12 NOPs
   - Reset DIV to 0
   - `HALT` - wait for immediate VBLANK
   - Should NOT reach `jp fail_round3`
3. Read DIV → save to B register
4. Expected value: $11 (17 decimal)

### Round 4: HALT immediately different timing (Expected: $12)
1. Set handler to `test_round4`, then `finish_round4`
2. Execute sequence:
   - 11 NOPs
   - Reset DIV to 0
   - `HALT` - wait for immediate VBLANK
   - Should NOT reach `jp fail_round4`
3. Read DIV → save to C register
4. Expected value: $12 (18 decimal)

### Final Assertions
```
assert_b $11
assert_c $12
assert_d $11
assert_e $12
```

## What The Test Expects

The test expects these final register values:
- **B = $11** (from Round 3)
- **C = $12** (from Round 4)
- **D = $11** (from Round 1)
- **E = $12** (from Round 2)

These values represent DIV register readings after specific timing sequences. The DIV register increments every 256 T-cycles (64 M-cycles), so these values indicate precise cycle counts elapsed.

The test specifically validates that:
1. HALT doesn't add extra cycles when servicing interrupts
2. Timing with HALT is identical to timing with NOPs
3. The difference between round pairs (11 vs 12 NOPs) produces exactly 1 DIV increment difference

## What The Test Is Testing

This test validates the **HALT instruction timing behavior with IME=1**:

1. **HALT servicing speed**: When IME=1 and an interrupt is pending, HALT should immediately service the interrupt without adding delay cycles

2. **Interrupt timing consistency**: The timing from `ei; halt` followed by an interrupt should be identical to `ei; nop` followed by an interrupt

3. **No HALT overhead**: The CPU should not spend extra cycles "waking up" from HALT in the IME=1 case - it should behave as if HALT was a NOP

4. **Precise cycle-accurate behavior**: The test uses DIV register (increments every 256 T-cycles) to measure exact timing

## Potential Failure Reasons

### 1. HALT Implementation - Extra Cycles During Interrupt Service

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Halt.java`

The current implementation:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    boolean IME = cpuStructure.registers().IME();
    boolean interruptsPending = cpuStructure.interruptBus().hasInterrupts();

    if (!IME && interruptsPending) {
        cpuStructure.idu().disableNextIncrement();
    }

    cpuStructure.clock().stop();
    cpuStructure.interruptBus().waitForInterrupt();  // <-- Issue here
    cpuStructure.clock().start();
}
```

**Problem**: The `waitForInterrupt()` method actively ticks the clock while waiting:
```java
public void waitForInterrupt() {
    while (!hasInterrupts()) {
        clock.tick();  // <-- Ticks clock during wait
    }
}
```

**Issue**: When IME=1 and an interrupt is already pending (or becomes pending during HALT), the test expects HALT to behave **exactly like a NOP** - taking only 4 T-cycles (1 M-cycle) total. However:

1. The current implementation may tick the clock during `waitForInterrupt()`
2. After returning from `waitForInterrupt()`, the normal fetch cycle happens
3. This could add extra cycles that wouldn't exist with a NOP

### 2. Clock Stop/Start Behavior

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/SynchronisedClock.java`

```java
@Override
public void stop() {
    // Empty - does nothing
}

@Override
public void start() {
    // Empty - does nothing
}
```

**Problem**: The stop/start methods don't actually do anything, so `cpuStructure.clock().stop()` and `cpuStructure.clock().start()` have no effect. This means the clock continues running during HALT, which could cause timing discrepancies.

### 3. Interrupt Handling After HALT

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java`

The normal interrupt handling happens in `handlePotentialInterrupt()` which is called during the fetch cycle:
```java
private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();

        handlePotentialInterrupt();  // <-- Called after fetch + tick

        instruction.postFetch(cpuStructure);
    }
}
```

**Issue**: When HALT completes and returns, the normal fetch cycle continues with `handlePotentialInterrupt()`. But HALT has already consumed time in `waitForInterrupt()`, so the total cycle count may not match the expected NOP timing.

### 4. Missing HALT-Specific Interrupt Timing

**Expected behavior**: On real hardware, when IME=1:
- `HALT` takes 4 T-cycles (1 M-cycle) to execute
- If an interrupt is pending, it's serviced immediately after HALT
- Total timing: 1 M-cycle for HALT + 5 M-cycles for interrupt service = 6 M-cycles

**Current behavior**: The emulator may be adding extra cycles in `waitForInterrupt()` or not properly accounting for the HALT execution time, causing the total elapsed cycles to differ from what a NOP would produce.

### 5. EI Delay Not Interacting Correctly with HALT

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/EnableInterrupts.java`

The `EI` instruction enables interrupts in `postFetch()`, meaning IME is set after the next instruction fetch:
```java
@Override
public void postFetch(CpuStructure cpuStructure) {
    cpuStructure.registers().setIME(true);
}
```

This creates the sequence: `ei` → fetch next instruction → set IME=1 → execute next instruction

**Issue**: The test executes `ei; halt; nop`. The HALT should execute with IME=0 (not yet enabled), but IME becomes 1 during the HALT execution or right after. The timing of when IME=1 takes effect relative to HALT's interrupt checking could cause discrepancies.

### 6. DIV Register Increment Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/components/InternalTimerCounter.java`

The DIV register is the upper 8 bits of the internal counter:
```java
@Override
public byte read() {
    return (byte) ((internalCounter.getValue() >> 8) & 0xFF);
}
```

The counter increments every T-cycle (every 4 M-cycles). If HALT is consuming extra cycles beyond what a NOP would, the DIV readings will be off by 1 or more.

## Summary

The test is likely failing because **HALT is not cycle-accurate when IME=1 and interrupts are pending**. The emulator's HALT implementation actively waits for interrupts by ticking the clock in a loop, which may add extra cycles compared to a NOP instruction. The test expects HALT to take exactly 4 T-cycles (1 M-cycle) before the interrupt is serviced, just like a NOP would. Any deviation in timing causes the DIV register readings to be incorrect, failing the assertions.

To fix this, the HALT implementation needs to:
1. Not add extra cycles when IME=1 and interrupts are pending
2. Properly handle the case where an interrupt becomes pending immediately
3. Ensure total timing matches: HALT (4 T-cycles) + interrupt service (20 T-cycles) = 24 T-cycles total
