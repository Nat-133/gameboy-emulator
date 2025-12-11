# Mooneye Test Analysis: jp_timing

## Test Overview
The `jp_timing` test validates the precise timing behavior of the `JP nn` (Jump to immediate 16-bit address) instruction, specifically testing the timing of memory reads during the instruction's execution in relation to OAM DMA transfers.

## What The Test Does

### Setup Phase
1. **Disable interrupts** (`di`)
2. **Wait for VBLANK**
3. **Copy test code to multiple memory locations:**
   - Copies 16 bytes starting from `(wram_test + 2)` to VRAM at address `$8000`
   - Copies 16 bytes from `wram_test` to OAM-2 (address `$FDFE`)
   - The `wram_test` contains a `JP $1a00` instruction (3 bytes: `C3 00 1A`)
4. **Copy test procedure to HIRAM** (`$FF80-$FFDF`, 96 bytes)
5. **Jump to HIRAM** to execute the test

### Test Execution

The test runs in two rounds to verify the exact cycle timing of the `JP nn` instruction's memory reads:

#### Round 1: Testing Memory Read One Cycle Before DMA End

1. **Set up the low byte of the jump address:**
   - Write `$CA` to address `$FDFF` (OAM - 1)
   - This means the `JP` instruction's first byte (low byte of `nn`) at `$FDFE` will be `$CA`

2. **Start OAM DMA** from source `$8000`:
   - Initiates DMA transfer to copy 160 bytes from `$8000-$809F` to OAM (`$FE00-$FE9F`)
   - OAM DMA takes 160 machine cycles to complete (one byte per M-cycle)

3. **Delay to align timing:**
   - Executes `dec b` 38 times in a loop (38 * 4 = 152 M-cycles for loop)
   - Plus loop overhead
   - Plus 3 NOPs
   - Plus `ld hl, OAM - 2` (3 M-cycles)
   - **Total timing aligns so that the high byte read of `JP nn` happens exactly 1 M-cycle BEFORE OAM DMA finishes**

4. **Execute `JP HL`** where HL = `$FDFE`:
   - This jumps to the `JP nn` instruction at `$FDFE`
   - The `JP nn` instruction reads:
     - **M-cycle 0:** Instruction decode (opcode `$C3` at `$FDFE`)
     - **M-cycle 1:** Read low byte from `$FDFF` → reads `$CA`
     - **M-cycle 2:** Read high byte from `$FE00` → **happens during DMA (1 cycle before end)**
     - **M-cycle 3:** Internal delay

5. **Expected behavior:**
   - During DMA, reading from OAM returns `$FF`
   - So the high byte read = `$FF`
   - The jump becomes `JP $FFCA`
   - At `$FFCA` there's code that continues to Round 2

#### Round 2: Testing Memory Read After DMA End

1. **Set up the low byte of the jump address:**
   - Write `$DA` to address `$FDFF` (OAM - 1)

2. **Start OAM DMA** from source `$8000` again

3. **Delay with different timing:**
   - Same 38-iteration loop
   - But **4 NOPs** instead of 3
   - Plus `ld hl, OAM - 2`
   - **Total timing aligns so that the high byte read of `JP nn` happens exactly AFTER OAM DMA finishes**

4. **Execute `JP HL`** where HL = `$FDFE`:
   - The `JP nn` instruction reads:
     - **M-cycle 0:** Instruction decode
     - **M-cycle 1:** Read low byte from `$FDFF` → reads `$DA`
     - **M-cycle 2:** Read high byte from `$FE00` → **happens after DMA ends**
     - **M-cycle 3:** Internal delay

5. **Expected behavior:**
   - After DMA ends, OAM is accessible normally
   - The high byte at `$FE00` contains actual OAM data copied from VRAM
   - Looking at the setup: VRAM was loaded with data from `(wram_test + 2)`
   - The `wram_test` is the `JP $1a00` instruction: `C3 00 1A`
   - So `(wram_test + 2)` starts with `$1A`
   - After DMA, `$FE00` contains `$1A`
   - The jump becomes `JP $1ADA`
   - At `$1ADA` there's code that finishes the test successfully

### Success/Failure Paths

- If Round 1 high byte is NOT `$FF` → jumps to `$1ACA` → **FAIL: ROUND 1**
- If Round 1 succeeds but Round 2 high byte is NOT `$1A` → jumps to `$FFDA` → **FAIL: ROUND 2**
- If both rounds succeed → jumps to `$1ADA` → **Test OK**

## What The Test Expects

### Round 1 Expectations
- When `JP nn` reads the high byte from `$FE00` **during active OAM DMA** (one cycle before end), it should read `$FF` (the blocked memory value)
- This causes the jump to `$FFCA` (finish_round1)

### Round 2 Expectations
- When `JP nn` reads the high byte from `$FE00` **after OAM DMA completes**, it should read `$1A` (the actual OAM data)
- This causes the jump to `$1ADA` (finish_round2, then test_finish)

### Timing Requirements
The test verifies that:
1. **JP nn instruction takes exactly 4 M-cycles with the following breakdown:**
   - M-cycle 0: Instruction decode
   - M-cycle 1: Read low byte of address
   - M-cycle 2: Read high byte of address
   - M-cycle 3: Internal delay before jumping

2. **Memory reads happen at specific M-cycles:**
   - The high byte read must occur at M-cycle 2 of the instruction
   - Memory blocking by DMA must be respected during this read
   - Memory blocking must end at the precise M-cycle when DMA finishes

## What The Test Is Testing

This test validates:

1. **JP nn Instruction Timing:**
   - The instruction must take exactly 4 M-cycles
   - Memory reads must happen in the correct M-cycles (cycle 1 and 2)
   - The internal delay must be in M-cycle 3

2. **OAM DMA Memory Blocking Behavior:**
   - During DMA transfer, reads from blocked memory regions (including OAM) must return `$FF`
   - DMA blocking must end precisely after 160 M-cycles
   - The transition from blocked to unblocked must happen at the exact M-cycle

3. **Memory Read/DMA Synchronization:**
   - The test verifies that memory reads during instruction execution properly interact with DMA state
   - Specifically testing the boundary condition of reading exactly 1 cycle before vs. after DMA completion

## Potential Failure Reasons

### 1. Incorrect JP Instruction Timing

**Current Implementation (`Jump.java`, lines 59-70):**
```java
private void executeJpImm16(CpuStructure cpuStructure) {
    byte Z = ControlFlow.readIndirectPCAndIncrement(cpuStructure);  // M-cycle 1

    boolean doJump = evaluateCondition(cc, cpuStructure.registers());
    byte W = ControlFlow.readIndirectPCAndIncrement(cpuStructure);  // M-cycle 2
    short imm16 = BitUtilities.concat(W, Z);

    if (doJump) {
        cpuStructure.registers().setPC(imm16);
        cpuStructure.clock().tick();  // M-cycle 3
    }
}
```

**Analysis:**
- The low byte read happens in M-cycle 1 (via `readIndirectPCAndIncrement`)
- The high byte read happens in M-cycle 2 (via second `readIndirectPCAndIncrement`)
- The internal delay (M-cycle 3) happens via `cpuStructure.clock().tick()`
- **This appears correct for the basic timing**

**Potential Issues:**
- The condition evaluation happens BETWEEN the two memory reads, but this shouldn't affect timing
- For unconditional JP (when `cc == null`), the condition evaluation is trivial
- The internal delay only happens when the jump is taken, but for this test the jump is always taken

### 2. DMA Timing and Memory Blocking

**Current Implementation (`MemoryBus.java`):**

**DMA State Machine (lines 89-105):**
```java
@Override
public void mCycle() {
    switch (dmaPhase) {
        case INACTIVE -> {}
        case REQUESTED -> dmaPhase = DmaPhase.PENDING;
        case PENDING -> dmaPhase = DmaPhase.TRANSFERRING;
        case TRANSFERRING -> {
            short sourceAddr = (short) (dmaSourceAddress + dmaByteIndex);
            short destAddr = (short) (OAM_START_ADDRESS + dmaByteIndex);
            byte data = underlying.read(sourceAddr);
            underlying.write(destAddr, data);
            dmaByteIndex++;
            if (dmaByteIndex >= OAM_SIZE) {  // OAM_SIZE = 160
                dmaPhase = DmaPhase.INACTIVE;
            }
        }
    }
}
```

**Memory Blocking (lines 61-66, 69-75):**
```java
@Override
public byte read(short address) {
    ensureDmaListenerRegistered();
    if (isBlocking() && !isAccessibleDuringDma(address)) {
        return (byte) 0xFF;
    }
    return underlying.read(address);
}

private boolean isBlocking() {
    return switch (dmaPhase) {
        case INACTIVE -> false;
        case REQUESTED, PENDING -> blockingDuringSetup;
        case TRANSFERRING -> true;
    };
}
```

**Analysis:**
The DMA implementation has a three-phase startup:
1. **REQUESTED:** Same cycle as write to $FF46
2. **PENDING:** One M-cycle delay
3. **TRANSFERRING:** Actually transferring bytes

**Critical Issue:**
The DMA phase transitions happen at the START of each M-cycle (in the `mCycle()` method), but memory reads happen DURING the M-cycle. This creates a timing ambiguity:

**Scenario:** Reading during the M-cycle when DMA completes
- At the start of M-cycle 163 (counting from DMA start):
  - `dmaByteIndex = 159` (last byte)
  - `dmaPhase = TRANSFERRING`
  - `isBlocking() = true`
- During M-cycle 163:
  - The byte at index 159 is transferred
  - `dmaByteIndex++` → becomes 160
  - `dmaPhase = INACTIVE` (transfer complete)
- **Question:** If a memory read happens during this same M-cycle (after the DMA phase update), what should it return?

**The Test's Expectation:**
Based on the test comments:
- Round 1: "the memory read of nn is aligned to happen exactly one cycle **before** the OAM DMA end" → should return `$FF`
- Round 2: "the memory read of nn is aligned to happen exactly **after** OAM DMA ends" → should return actual data

**Problem:**
The test needs to distinguish between:
1. Reading during the M-cycle that completes DMA (should still be blocked?)
2. Reading during the next M-cycle after DMA completes (should not be blocked)

**In the current implementation:**
- The `mCycle()` method runs during `clock.tick()` via `ClockWithParallelProcess`
- The DMA state update happens when the clock ticks
- Memory reads that happen during the same M-cycle might see either the old or new DMA state depending on ordering

### 3. Clock Tick Ordering

**Current Implementation (`ClockWithParallelProcess.java`, lines 12-16):**
```java
@Override
public void tick() {
    parallelProcess.run();  // This calls dmaController.mCycle()
    time++;
}
```

**From `CpuModule.java`, lines 60-67:**
```java
return new ClockWithParallelProcess(() -> {
    timer.mCycle();
    serialController.mCycle();
    dmaController.mCycle();
    for (int i = 0; i < 4; i++) {
        ppu.tCycle();
    }
});
```

**Analysis:**
When a CPU instruction does `clock.tick()`:
1. The parallel process runs (including `dmaController.mCycle()`)
2. Time increments

This means DMA state updates happen at the START of each M-cycle, before any memory operations within that M-cycle.

**Problem:**
If an instruction does:
```java
byte value = cpuStructure.memory().read(address);  // Read happens here
cpuStructure.clock().tick();  // DMA updates here
```

The memory read happens BEFORE the DMA state update, so it sees the OLD DMA state.

But if an instruction does:
```java
cpuStructure.clock().tick();  // DMA updates here
byte value = cpuStructure.memory().read(address);  // Read happens here
```

The memory read happens AFTER the DMA state update, so it sees the NEW DMA state.

**In `readIndirectPCAndIncrement`:**
```java
public static byte readIndirectPCAndIncrement(CpuStructure cpuStructure) {
    byte value = cpuStructure.memory().read(cpuStructure.registers().PC());  // Read FIRST
    incrementPC(cpuStructure);
    cpuStructure.clock().tick();  // Clock tick AFTER
    return value;
}
```

The memory read happens BEFORE the clock tick, meaning it sees the DMA state from the PREVIOUS M-cycle.

**This might be the issue:**
- When the test expects a read to happen "during" M-cycle N, it might actually be seeing the DMA state from M-cycle N-1
- The test's careful timing might be off by one M-cycle due to this ordering

### 4. Detailed Timing Analysis

Let's trace through Round 1 more carefully:

**DMA Start:**
- At M-cycle 0: DMA write triggers, phase = REQUESTED
- At M-cycle 1: `mCycle()` runs, phase → PENDING
- At M-cycle 2: `mCycle()` runs, phase → TRANSFERRING, byte 0 transferred
- At M-cycle 3: byte 1 transferred
- ...
- At M-cycle 161: byte 159 transferred, phase → INACTIVE

**The JP HL and JP nn execution:**
Assume `JP HL` at M-cycle 158 (this is what the test aims for with its timing):
- M-cycle 158: Fetch and execute `JP HL`, PC set to `$FDFE`
- M-cycle 159: Fetch `JP nn` opcode at `$FDFE` (opcode = `$C3`)
- M-cycle 160: Execute `JP nn`:
  - Read low byte from `$FDFF` (read happens before clock.tick)
  - Clock ticks → DMA transfers byte 158 (at index 158), still TRANSFERRING
- M-cycle 161: Continue `JP nn`:
  - Read high byte from `$FE00` (read happens before clock.tick)
  - Clock ticks → DMA transfers byte 159 (at index 159), becomes INACTIVE
- M-cycle 162: Continue `JP nn`:
  - Internal delay (clock.tick only)
  - Clock ticks → DMA is INACTIVE

**At the high byte read in M-cycle 161:**
- The read happens at the START of the M-cycle (before `clock.tick`)
- At that point, DMA phase is still TRANSFERRING (from M-cycle 160's update)
- So `isBlocking() = true`
- The read returns `$FF` ✓ **This is correct for Round 1**

**For Round 2 with 4 NOPs (one extra M-cycle delay):**
- M-cycle 161: Continue `JP nn`:
  - Read high byte from `$FE00` (read happens before clock.tick)
  - At this point, DMA is INACTIVE (because the extra NOP delayed everything)
- So `isBlocking() = false`
- The read returns actual OAM data ✓ **This should be correct for Round 2**

**Wait, this analysis suggests the implementation might be correct!**

### 5. Re-examining the Fetch Cycle

Looking at `Cpu.java` more carefully:

```java
public void cycle() {
    Instruction instruction = decode(cpuStructure.registers().instructionRegister());

    instruction.execute(cpuStructure);

    fetch_cycle(instruction);
}

private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();  // Fetch cycle tick

        handlePotentialInterrupt();

        instruction.postFetch(cpuStructure);
    }
}
```

**The fetch cycle happens AFTER instruction execution!**

So the M-cycle breakdown for `JP nn` would be:
- M-cycle 0: **PREVIOUS instruction's fetch** (reads opcode `$C3` for JP nn)
- M-cycle 1: **Execute JP nn** - read low byte, tick
- M-cycle 2: **Execute JP nn** - read high byte, tick
- M-cycle 3: **Execute JP nn** - internal delay (if jumping), tick
- M-cycle 4: **JP nn's fetch** (reads next opcode at new PC)

**But the test documentation says:**
- M = 0: instruction decoding
- M = 1: nn read: memory access for low byte
- M = 2: nn read: memory access for high byte
- M = 3: internal delay

**This suggests the opcode fetch is M-cycle 0, not a separate "previous" cycle.**

### 6. The Real Issue: JP HL Timing

Looking back at the test, it executes `JP HL` to jump to the `JP nn` instruction:

```java
private static void executeJpHL(CpuStructure cpuStructure) {
    cpuStructure.registers().setPC(cpuStructure.registers().HL());
}
```

**Problem:** `JP HL` doesn't call `clock.tick()` for the internal operation!

`JP HL` should take 1 M-cycle according to Game Boy documentation:
- M = 0: instruction decoding (fetch of JP HL opcode, already done)
- The actual PC update is internal and happens in the same M-cycle as the decode

But in the current implementation:
- The fetch happens in the previous instruction's fetch_cycle
- The execute does NOT tick the clock
- The next fetch_cycle ticks once

So `JP HL` effectively takes 1 M-cycle (the fetch of the next instruction), which seems correct.

### 7. Conditional Jump Timing

Looking more carefully at `executeJpImm16`:

```java
if (doJump) {
    cpuStructure.registers().setPC(imm16);
    cpuStructure.clock().tick();  // M-cycle 3
}
```

**The internal delay tick only happens if the jump is taken!**

For an unconditional `JP nn`:
- `doJump = evaluateCondition(null, ...)` returns `true`
- So the tick DOES happen

This seems correct.

### 8. The Most Likely Issue: Off-by-One in DMA Timing

Let me recalculate the exact M-cycle when DMA finishes:

**DMA phases:**
- Write to $FF46: phase = REQUESTED (happens during the write, not in mCycle)
- M-cycle after write: `mCycle()` called, phase → PENDING, blocking may start
- Next M-cycle: `mCycle()` called, phase → TRANSFERRING, byte 0 transferred, byteIndex = 1
- Next M-cycle: byte 1 transferred, byteIndex = 2
- ...
- M-cycle with byteIndex = 159: byte 159 transferred, byteIndex = 160, phase → INACTIVE

So if we count from the write to $FF46 as M-cycle 0:
- M-cycle 1: phase → PENDING
- M-cycle 2: phase → TRANSFERRING, transfer byte 0
- M-cycle 3: transfer byte 1
- ...
- M-cycle 161: transfer byte 159, phase → INACTIVE

The test expects OAM DMA to take 160 M-cycles after the initiation. The current implementation seems to match this (2 setup cycles + 160 transfer cycles = 162 total, but only 160 during actual TRANSFERRING).

**Actually, wait:**

Looking at the `start_oam_dma` macro:
```
.macro start_oam_dma ARGS address
  wait_vblank
  ld a, address
  ldh (<DMA), a
.endm
```

The write to DMA happens via `ldh (<DMA), a`, which takes 3 M-cycles total:
- M-cycle 0: Fetch opcode
- M-cycle 1: Read immediate byte (offset)
- M-cycle 2: Write to memory

The DMA is triggered by the write, which happens in M-cycle 2.

So counting from the cycle where the write happens:
- M-cycle 0 (write happens): phase = REQUESTED
- M-cycle 1: `mCycle()`, phase → PENDING
- M-cycle 2: `mCycle()`, phase → TRANSFERRING, byte 0, index → 1
- ...
- M-cycle 161: byte 159, index → 160, phase → INACTIVE

The blocking should be active from M-cycle 1 through M-cycle 161.

## Summary of Findings

The `jp_timing` test is a precision timing test that validates:

1. The `JP nn` instruction takes exactly 4 M-cycles with memory reads in M-cycles 1 and 2
2. OAM DMA properly blocks memory access and returns `$FF` for blocked regions
3. DMA blocking ends at the precise M-cycle when the transfer completes
4. The boundary between blocked and unblocked memory is exact (testing 1 cycle before vs. after DMA completion)

**Potential Issues in the Emulator:**

1. **Most Likely:** The timing relationship between memory reads and DMA state updates may be off by one M-cycle due to the order of operations in `clock.tick()` and `readIndirectPCAndIncrement()`.

2. **DMA Phase Transitions:** The transition from TRANSFERRING to INACTIVE happens when `dmaByteIndex >= 160`, but memory reads in the same M-cycle see the state BEFORE this transition due to read-before-tick ordering.

3. **Blocking Logic:** The `isBlocking()` check might not properly handle the boundary M-cycle when DMA completes.

4. **Startup Delay:** The two-phase startup (REQUESTED → PENDING → TRANSFERRING) adds 2 M-cycles before transfers begin. The test might expect different timing.

**Recommended Investigation:**

1. Add detailed logging to track the exact M-cycle when:
   - DMA phase changes occur
   - Memory reads from OAM happen
   - The transition from blocked to unblocked occurs

2. Verify that memory reads during instruction execution see the correct DMA state for that M-cycle

3. Consider whether memory reads should happen before or after the DMA state update within each M-cycle

4. Test with the actual ROM to see which round fails, which would pinpoint whether the issue is with "during DMA" or "after DMA" behavior
