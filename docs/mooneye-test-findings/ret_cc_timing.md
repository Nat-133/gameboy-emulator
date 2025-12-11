# Mooneye Test Analysis: ret_cc_timing

## Test Overview

The `ret_cc_timing` test verifies the precise timing behavior of the `RET cc` (conditional return) instruction on the Game Boy. It tests when memory reads occur during the execution of the RET instruction when the condition is true, using OAM DMA's memory access restrictions to determine the exact machine cycle timing. The test specifically verifies that RET cc takes 5 machine cycles total when the condition is met.

## What The Test Does

### Setup Phase

1. **HIRAM Callback Setup**:
   - Copies a 2-byte callback sequence to HIRAM at `$FF80`:
     ```asm
     xor a      ; Clear A register (sets A = 0, Z flag = 1)
     jp hl      ; Jump to address in HL
     ```

### Test Round 1: Testing Memory Read Timing During OAM DMA

1. **Memory Configuration**:
   - Waits for VBLANK
   - Writes `$80` to address `OAM - 1` (`$FDFF`)
   - Writes `$20` to VRAM at address `$8000`
   - Sets SP to `OAM - 1` (`$FDFF`), so the stack pointer points to where we wrote `$80`
   - Sets HL to `finish_round1` (the return address we want to validate)

2. **OAM DMA Timing Setup**:
   - Starts OAM DMA from source address `$80` (copies from `$8000` to OAM)
   - Executes a delay loop: 39 iterations of `dec b; jr nz`
   - Adds 2 NOPs after the loop
   - This timing is crucial to align the RET instruction's memory reads with specific DMA phases

3. **RET Z Execution**:
   - Executes `RET Z` instruction
   - Z flag is set (from previous state), so the condition is TRUE and the return should execute
   - The RET cc instruction timing (according to test comments):
     - M=0: Instruction decoding
     - M=1: Internal delay
     - M=2: PC pop - memory access for low byte (from address in SP)
     - M=3: PC pop - memory access for high byte (from address in SP+1)
     - M=4: Internal delay

4. **The Critical Test**:
   - SP is `$FDFF` (OAM - 1)
   - The RET instruction will try to pop PC from the stack:
     - Low byte from `$FDFF` (OAM - 1)
     - High byte from `$FE00` (first byte of OAM)
   - The timing is carefully aligned so that M=2 memory read happens during or after DMA
   - If M=2 read from `$FDFF` happens when DMA is blocking: CPU reads `$FF` instead of `$80`
   - If M=3 read from `$FE00` happens when DMA is blocking: CPU reads `$FF` instead of the value DMA wrote
   - Depending on when exactly the reads happen, PC will be different

5. **Memory Layout at Address `$2000`-`$2100`**:
   - At address `$2080` (high byte `$20`):
     ```asm
     ld a, $01
     jp hl
     ```
   - At HIRAM `$FF80`:
     ```asm
     xor a      ; Sets A = 0
     jp hl
     ```

6. **Expected Behavior for Round 1**:
   - With 2 NOPs, the timing should cause PC to be read as `$FF80` (reading `$FF` and `$80`)
   - This jumps to HIRAM callback which does `xor a` (sets A=0) then `jp hl`
   - The test then checks `or a` - if A is 0, Z flag is set, and we proceed to round 2
   - If timing is wrong and PC is `$2080` instead, it does `ld a, $01`, making A non-zero
   - Then `or a` would show A=1, and the test would fail with "FAIL: ROUND 1"

### Test Round 2: Inverse Timing Test

1. **Memory Configuration**:
   - Waits for VBLANK again
   - Writes `$FF` to address `OAM` (`$FE00`) - note: not OAM-1 this time
   - Sets SP to `OAM - 1` (`$FDFF`)
   - Sets HL to `finish_round2`

2. **OAM DMA Timing Setup**:
   - Starts OAM DMA from source address `$80`
   - Executes delay loop: 39 iterations
   - Adds **3 NOPs** this time (one more than round 1)

3. **RET Z Execution**:
   - Executes `RET Z` with Z flag set
   - Same stack pointer `$FDFF`
   - With one additional NOP, the timing shifts by 1 M-cycle

4. **Expected Behavior for Round 2**:
   - With 3 NOPs, the timing should cause PC to be read as `$2080` (reading `$20` and `$80`)
   - This jumps to address `$2080` which does `ld a, $01` (sets A=1) then `jp hl`
   - The test then checks `or a` - A=1 means Z flag is NOT set
   - Then `jr nz, test_success` succeeds
   - If timing is wrong and PC is `$FF80` instead, A would be 0, and test fails with "FAIL: ROUND 2"

## What The Test Expects

The test expects very precise timing for the RET cc instruction:

### When Condition is Met (Z flag set):

1. **Total Duration**: 5 machine cycles
2. **Memory Read Timing**:
   - M=0: Instruction decoding
   - M=1: Internal delay (condition evaluation)
   - M=2: PC low byte pop from memory at SP
   - M=3: PC high byte pop from memory at SP+1
   - M=4: Internal delay (before PC is actually used)

### Round 1 Success Criteria:
- With 2 NOPs of delay, memory reads at M=2 and M=3 should happen **during** OAM DMA blocking period
- This causes high byte to read as `$FF` from blocked memory
- Result: PC becomes `$FF80`, A becomes 0 after callback

### Round 2 Success Criteria:
- With 3 NOPs of delay (1 M-cycle later), memory reads should happen **after** OAM DMA completes
- This causes reads to succeed normally
- Result: PC becomes `$2080`, A becomes 1

## What The Test Is Testing

This test validates:

1. **RET cc Instruction Timing**: The total number of machine cycles (5 M-cycles when condition is true)
2. **Memory Read Phases**: The specific M-cycles when memory reads occur (M=2 for low byte, M=3 for high byte)
3. **Internal Delays**: Confirms there's an internal delay at M=1 (condition checking) and M=4 (before PC update)
4. **DMA Interaction**: Verifies that memory reads respect DMA blocking behavior correctly

The test uses a technique where:
- OAM DMA blocks memory access from M-cycle range [start + 2, start + 161]
- By varying the delay by 1 M-cycle (2 vs 3 NOPs), the test checks if reads shift accordingly
- The difference in outcomes (A=0 vs A=1) proves the reads happened at different times

## Potential Failure Reasons

### 1. Incorrect Cycle Count in ConditionalReturn Implementation

**Current Implementation** (`ConditionalReturn.java`):
```java
@Override
public void execute(CpuStructure cpuStructure) {
    boolean shouldReturn = ControlFlow.evaluateCondition(condition, cpuStructure.registers());
    cpuStructure.clock().tick();  // M=1: internal delay

    if (shouldReturn) {
        short value = ControlFlow.popFromStack(cpuStructure);
        cpuStructure.registers().setPC(value);
        cpuStructure.clock().tick();  // M=4: internal delay
    }
}
```

**Analysis**:
- M=0: Instruction decoding (happens before execute() is called)
- M=1: First tick - internal delay for condition evaluation
- M=2-3: `popFromStack()` should take 2 M-cycles (M=2 and M=3)
- M=4: Second tick - internal delay before PC is used

Let's verify `popFromStack()` timing in `ControlFlow.java`:

```java
public static short popFromStack(CpuStructure cpuStructure) {
    byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
    incrementSP(cpuStructure);

    cpuStructure.clock().tick();  // M=2

    byte msb = cpuStructure.memory().read(cpuStructure.registers().SP());
    incrementSP(cpuStructure);

    cpuStructure.clock().tick();  // M=3

    return concat(msb, lsb);
}
```

**Cycle Count Verification**:
- M=0: Instruction decoding (automatic)
- M=1: Condition evaluation tick in ConditionalReturn
- M=2: First tick in popFromStack (after low byte read)
- M=3: Second tick in popFromStack (after high byte read)
- M=4: Final tick in ConditionalReturn (after setting PC)

**Total: 5 M-cycles** - This matches the expected timing!

### 2. Memory Read Timing During DMA

The critical question is: **When exactly do the memory reads happen relative to the clock ticks?**

In the current implementation:
```java
byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());  // Read happens HERE
incrementSP(cpuStructure);
cpuStructure.clock().tick();  // M=2 tick happens AFTER the read
```

**Issue**: The memory read for the low byte happens **before** the M=2 tick, not during it.

According to real hardware:
- M=2: Memory read for low byte should happen **during** this M-cycle
- M=3: Memory read for high byte should happen **during** this M-cycle

But in the emulator:
- Low byte read happens before M=2 tick (at the end of M=1)
- High byte read happens before M=3 tick (at the end of M=2)

This means the reads are **1 M-cycle early** compared to when the ticks are recorded.

### 3. DMA Blocking Window

The `MemoryBus` implementation shows:
```java
private boolean isBlocking() {
    return switch (dmaPhase) {
        case INACTIVE -> false;
        case REQUESTED, PENDING -> blockingDuringSetup;
        case TRANSFERRING -> true;
    };
}
```

DMA goes through phases:
- INACTIVE → REQUESTED (on write to DMA register, same cycle)
- REQUESTED → PENDING (after 1 M-cycle)
- PENDING → TRANSFERRING (after 1 M-cycle)
- TRANSFERRING for 160 bytes (160 M-cycles)

So blocking starts at the TRANSFERRING phase, which begins 2 M-cycles after DMA is started.

### 4. Timing Analysis

Let's trace through Round 1 timing:

**Starting point**: `start_oam_dma $80` is called
- This is a macro that does `wait_vblank; ld a, $80; ldh (DMA), a`
- The write to DMA happens, DMA phase becomes REQUESTED

**Delay loop**: 39 iterations of `dec b; jr nz, -`
- `dec b`: 1 M-cycle
- `jr nz, -`: 3 M-cycles (when taken), 2 M-cycles (when not taken)
- 38 iterations taken: 38 × 4 = 152 M-cycles
- 1 iteration not taken: 1 + 2 = 3 M-cycles
- Total: 155 M-cycles

**2 NOPs**: 2 M-cycles

**Total delay**: 155 + 2 = 157 M-cycles after DMA write

**DMA phases during this time**:
- M=0: Write to DMA, phase = REQUESTED
- M=1: phase = PENDING
- M=2: phase = TRANSFERRING, blocking starts
- M=3-161: TRANSFERRING continues (160 bytes transferred)
- M=162: DMA completes, phase = INACTIVE

**RET Z begins at**: 157 M-cycles after DMA start
- M=0 (cycle 157): Instruction decode
- M=1 (cycle 158): Condition check + tick
- M=2 (cycle 159): Low byte read + tick
- M=3 (cycle 160): High byte read + tick
- M=4 (cycle 161): Final delay + tick

**Critical observation**:
- If low byte read happens **before** M=2 tick (at end of M=1), it's at cycle 157-158
- If high byte read happens **before** M=3 tick (at end of M=2), it's at cycle 158-159
- DMA is active (TRANSFERRING) from cycle 2 to cycle 161
- Both reads would be DURING DMA blocking window

But wait, let's recalculate more carefully with actual instruction timing:

Actually, the test timing needs to be analyzed from when `ret z` **starts executing**, not when DMA starts.

### 5. The Core Issue: Read-Before-Tick Pattern

The fundamental problem is likely the **order of operations** in the emulator:

**Current pattern** (problematic):
```java
// Read happens first
byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
// Tick happens after
cpuStructure.clock().tick();
```

**Expected pattern** (correct):
```java
// Tick should happen first to advance to the M-cycle
cpuStructure.clock().tick();
// Then read happens during that M-cycle
byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
```

This applies to ALL memory operations - the clock tick should happen **before** the memory access, not after.

However, looking at the current implementation, this pattern is consistent throughout the codebase. If we changed just RET cc, it would be inconsistent with other instructions.

### 6. Actual Probable Issue

Looking more carefully at the test and implementation, the issue is likely:

**The test expects RET cc to take exactly 5 M-cycles when condition is TRUE, with memory reads happening at precise M-cycle positions.**

The current implementation does have 5 M-cycles total, but the timing relationship between clock ticks and memory operations might cause the reads to happen at slightly different times than expected, making the DMA interaction test fail.

The test is **extremely** sensitive to timing - a difference of just 1 M-cycle in when the memory reads occur will cause one of the two rounds to fail.

### 7. Comparison with Other Instructions

Looking at the test findings for `call_cc_timing.md`, similar issues were identified for CALL instructions. The pattern is consistent: these timing tests verify that memory operations happen at very specific M-cycles within the instruction execution.

The issue is likely in the fundamental timing model of how the emulator handles:
1. When memory reads occur relative to clock ticks
2. How DMA blocking interacts with in-progress instructions
3. Whether there's any buffering or latching of memory operations

### Recommended Investigation Steps

1. **Run the test** and see which round fails (Round 1 or Round 2)
2. **Add logging** to track:
   - When DMA starts and ends
   - When RET instruction executes
   - When memory reads happen
   - Clock cycle counts at each step
3. **Compare timing** with expected DMA completion time
4. **Verify DMA phase transitions** happen at the right M-cycles

The test is essentially asking: "Do memory reads in RET cc happen at exactly M=2 and M=3 of the instruction, or are they off by a cycle?"
