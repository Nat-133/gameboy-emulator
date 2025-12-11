# jp_cc_timing Test Analysis

## Test Overview

The `jp_cc_timing` test verifies the precise timing behavior of the conditional jump instruction `JP cc, nn` on Game Boy hardware. Specifically, it tests that memory reads for the 16-bit address operand occur at exact machine cycles, and that these reads can be affected by OAM DMA transfers which block memory access.

## What The Test Does

### Test Setup Phase
1. **Disables interrupts** with `di`
2. **Waits for VBLANK** to ensure safe memory operations
3. **Copies test code to strategic memory locations**:
   - Copies bytes 2-17 of `wram_test` to VRAM ($8000+)
   - Copies bytes 0-15 of `wram_test` to OAM-2 ($FDFE)
   - The result: The `JP c, $1a00` instruction straddles the OAM boundary at $FDFE-$FE00
4. **Copies test procedure to HIRAM** ($FF80-$FFDF) where code can run during OAM DMA
5. **Jumps to HIRAM test procedure**

### Round 1 Execution Flow (Testing Memory Read During DMA)
Starting from `hiram_test` at $FF80:

1. **Sets up the low byte** of the jump target:
   - Writes $CA to address $FDFF (OAM - 1)
   - This is the low byte of the `nn` operand in the `JP c, $1a00` instruction

2. **Initiates OAM DMA** from source $8000 to OAM:
   - Sets register B to 38 (for timing loop)
   - Writes $80 to DMA register ($FF46)
   - DMA takes 160 machine cycles (640 T-cycles) to complete

3. **Precise timing alignment**:
   - Decrements B 38 times in a loop (38 * 2 = 76 cycles, accounting for the loop overhead)
   - Adds 2 NOPs (2 cycles)
   - Loads HL with $FDFE (OAM - 2) (3 cycles)
   - Sets carry flag with `scf` (1 cycle)
   - Executes `jp hl` (1 cycle to jump to $FDFE)

4. **The critical JP cc, nn execution**:
   - PC is now at $FDFE, pointing to the `JP c, $1a00` instruction
   - **M-cycle 0**: Opcode fetch (reads $DA = opcode for JP c, nn)
   - **M-cycle 1**: Read low byte of nn from $FDFF (should read $CA from OAM)
   - **M-cycle 2**: Read high byte of nn from $FE00 (OAM start)
   - **M-cycle 3**: Internal delay cycle (conditional taken)

5. **The timing trick**:
   - The test is timed so that M-cycle 2 (reading the high byte at $FE00) occurs **exactly one cycle before OAM DMA ends**
   - During OAM DMA, all memory except HRAM and I/O registers is blocked and returns $FF
   - So the high byte read returns $FF instead of $1A
   - The jump becomes `JP c, $FFCA` instead of `JP c, $1A00`

6. **Expected outcome**:
   - Jumps to $FFCA (in HIRAM)
   - This location contains the `finish_round1` code
   - Executes 2 NOPs and jumps to `test_round2` at $FF80 + offset

### Round 2 Execution Flow (Testing Memory Read After DMA)
Starting from `test_round2`:

1. **Sets up the low byte** differently:
   - Writes $DA to address $FDFF (OAM - 1)

2. **Initiates another OAM DMA** from source $8000:
   - Same B = 38 setup
   - Writes $80 to DMA register ($FF46)

3. **Slightly different timing**:
   - Decrements B 38 times (same as before)
   - Adds **3 NOPs** instead of 2 (one extra cycle)
   - Same HL, scf, jp hl sequence

4. **The critical JP cc, nn execution (Round 2)**:
   - PC is at $FDFE again
   - **M-cycle 0**: Opcode fetch
   - **M-cycle 1**: Read low byte from $FDFF (reads $DA)
   - **M-cycle 2**: Read high byte from $FE00 (OAM start)
   - **M-cycle 3**: Internal delay cycle

5. **The timing trick (Round 2)**:
   - This time, M-cycle 2 occurs **exactly after OAM DMA ends**
   - Normal memory access is restored
   - The high byte read returns the actual value $1A (from the copied data)
   - The jump becomes `JP c, $1ADA` as intended

6. **Expected outcome**:
   - Jumps to $1ADA
   - This location contains `finish_round2` code
   - Executes 2 NOPs and jumps to `test_finish`
   - Test passes with "Test OK"

### Failure Paths
- If timing is off in Round 1, the high byte might be read as $1A instead of $FF, causing a jump to $1ACA which contains `jp fail_round1`
- If timing is off in Round 2, the high byte might be read as $FF instead of $1A, causing a jump to $FFDA which contains `jp fail_round2`

## What The Test Expects

The test expects the following precise timing for `JP cc, nn` when the condition is true (carry flag set):

1. **M-cycle 0**: Instruction decode - fetch the opcode
2. **M-cycle 1**: Memory read for low byte of nn - reads from PC+1
3. **M-cycle 2**: Memory read for high byte of nn - reads from PC+2
4. **M-cycle 3**: Internal delay cycle (additional cycle when jump is taken)

The test specifically expects:
- The high byte read in Round 1 to occur during OAM DMA blocking (returning $FF)
- The high byte read in Round 2 to occur after OAM DMA ends (returning $1A)
- The difference is exactly 1 machine cycle between the two rounds

## What The Test Is Testing

This test validates several critical aspects of Game Boy emulation:

1. **JP cc, nn instruction timing**: The instruction must take exactly 4 machine cycles (16 T-cycles) when the condition is true

2. **Memory read ordering**: The low byte must be read before the high byte, each in separate machine cycles

3. **OAM DMA memory blocking**: During OAM DMA transfer:
   - All memory outside HRAM ($FF80-$FFFE) and I/O registers ($FF00-$FF7F) must return $FF
   - This includes OAM itself ($FE00-$FE9F)
   - The blocking must be precise to the machine cycle

4. **OAM DMA timing precision**:
   - DMA must take exactly 160 machine cycles
   - Memory blocking must end exactly when the transfer completes
   - The test can detect single-cycle timing errors

5. **Internal delay cycle**: When a conditional jump is taken, there must be an additional internal delay cycle after reading both address bytes

## Potential Failure Reasons

### Issue 1: Missing Internal Delay Cycle in Jump Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Jump.java` lines 59-70

```java
private void executeJpImm16(CpuStructure cpuStructure) {
    byte Z = ControlFlow.readIndirectPCAndIncrement(cpuStructure);  // M-cycle 1

    boolean doJump = evaluateCondition(cc, cpuStructure.registers());
    byte W = ControlFlow.readIndirectPCAndIncrement(cpuStructure);  // M-cycle 2
    short imm16 = BitUtilities.concat(W, Z);

    if (doJump) {
        cpuStructure.registers().setPC(imm16);
        cpuStructure.clock().tick();  // M-cycle 3 - internal delay
    }
}
```

**Analysis**: The implementation appears correct. It:
1. Reads the low byte Z (with clock tick in readIndirectPCAndIncrement)
2. Evaluates the condition
3. Reads the high byte W (with clock tick in readIndirectPCAndIncrement)
4. If jumping, adds one extra clock tick for the internal delay

However, the **condition evaluation happens BETWEEN the two byte reads**. Let me trace the exact cycle-by-cycle behavior:

**Current Implementation Cycle Breakdown**:
- M-cycle 0: Opcode fetch (handled by CPU cycle management)
- M-cycle 1: `readIndirectPCAndIncrement` reads low byte, ticks clock
- **Condition check happens here** (no clock tick)
- M-cycle 2: `readIndirectPCAndIncrement` reads high byte, ticks clock
- M-cycle 3: If taken, extra `clock.tick()` for internal delay

The issue is subtle: **the condition is evaluated BETWEEN the two memory reads**. According to the test expectations, the memory reads should happen in consecutive machine cycles without interruption.

### Issue 2: OAM DMA Timing Precision

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java` lines 89-105

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
            if (dmaByteIndex >= OAM_SIZE) {
                dmaPhase = DmaPhase.INACTIVE;
            }
        }
    }
}
```

**Analysis**: The DMA controller state machine:
1. **INACTIVE**: No DMA running
2. **REQUESTED**: Same cycle as write to $FF46 (cycle 0)
3. **PENDING**: First delay cycle (cycle 1)
4. **TRANSFERRING**: Transfer begins (cycles 2-161)
   - Each mCycle transfers one byte (160 bytes total = cycles 2-161)
   - After byte 160 (OAM_SIZE), transitions to INACTIVE

**OAM_SIZE = 160 (0xA0 bytes)**

**DMA Cycle Count**:
- Cycle 0: REQUESTED (write to DMA register)
- Cycle 1: PENDING (delay)
- Cycles 2-161: TRANSFERRING (160 bytes transferred)
- Cycle 162: Back to INACTIVE

Total: **162 machine cycles for the complete DMA process**

However, **memory blocking should end when DMA phase becomes INACTIVE**, which happens at the END of cycle 161 (when dmaByteIndex reaches 160 and phase transitions to INACTIVE).

### Issue 3: Memory Blocking During DMA

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java` lines 61-75

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

**Analysis**: Memory reads return $FF when:
- Phase is TRANSFERRING (always blocks)
- Phase is REQUESTED or PENDING (only if blockingDuringSetup is true, which happens when restarting DMA)

The blocking logic seems correct: during TRANSFERRING phase, all non-HRAM/non-IO memory returns $FF.

**The critical question**: When exactly does the blocking end?

Looking at the `mCycle()` method:
1. During cycle 161 (last transfer), the code is still in TRANSFERRING phase
2. At the end of cycle 161, after `dmaByteIndex++` makes it 160, the phase transitions to INACTIVE
3. On cycle 162, the next `mCycle()` call sees INACTIVE phase

**But when does a memory read happen relative to mCycle calls?**

Looking at how clock.tick() works (from CpuModule.java):
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

Every `clock.tick()` calls `dmaController.mCycle()`. This means:
- **BEFORE** each memory read in readIndirectPCAndIncrement, the clock ticks
- Clock tick advances DMA state
- Then the memory read happens

So the sequence for the high byte read would be:
1. Call `readIndirectPCAndIncrement`
2. Inside that function: read memory at PC
3. Increment PC
4. Call `clock.tick()`
5. Clock tick calls `dmaController.mCycle()`

**Wait, that's wrong!** Looking at the ControlFlow code more carefully:

```java
public static byte readIndirectPCAndIncrement(CpuStructure cpuStructure) {
    byte value = cpuStructure.memory().read(cpuStructure.registers().PC());  // Read FIRST
    incrementPC(cpuStructure);
    cpuStructure.clock().tick();  // Tick AFTER
    return value;
}
```

So the sequence is:
1. Memory read happens
2. PC increments
3. Clock ticks (which calls dmaController.mCycle())

This means **the memory read sees the DMA state from BEFORE the clock tick**.

### Issue 4: Exact Timing Calculation

Let's trace through Round 1 to see if the timing works out:

**OAM DMA starts when writing to $FF46:**
- This triggers `startDma()` which sets phase to REQUESTED
- dmaByteIndex = 0

**Cycle-by-cycle from DMA start:**

| Cycle | DMA Phase Before mCycle | DMA Phase After mCycle | Notes |
|-------|------------------------|----------------------|-------|
| 0 | REQUESTED | PENDING | Write to $FF46 happened |
| 1 | PENDING | TRANSFERRING | About to start transfers |
| 2-161 | TRANSFERRING | TRANSFERRING | Bytes 0-159 transferred |
| 161 | TRANSFERRING (byte 159) | INACTIVE | Last byte transferred, phase changes |
| 162+ | INACTIVE | INACTIVE | Normal memory access |

**Round 1 Timing from the test:**
After starting DMA:
1. B = 38, start loop
2. Loop: `dec b` (1 cycle) + `jr nz` (3 cycles when taken, 2 when not)
   - 37 iterations: 37 * 4 = 148 cycles
   - Last iteration: 1 (dec) + 2 (jr not taken) = 3 cycles
   - Total: 151 cycles
3. 2 NOPs: 2 cycles
4. `ld hl, OAM-2`: 3 cycles
5. `scf`: 1 cycle
6. `jp hl`: 1 cycle (jumps to $FDFE)
7. Opcode fetch at $FDFE: 1 cycle
8. Read low byte at $FDFF: 1 cycle
9. Read high byte at $FE00: 1 cycle (THIS is the critical read)

Wait, I need to recalculate. Each "cycle" I've been counting might be an M-cycle (4 T-cycles).

Let me recalculate in M-cycles:

Actually, the test comment says the loop is calibrated with B=38 and the dec/jr loop. Let me trust the test's timing and work backwards.

The test expects the high byte read to happen "exactly one cycle before the OAM DMA end". If DMA ends at cycle 161 (when it transitions from TRANSFERRING to INACTIVE), then the high byte read should happen at cycle 160.

But there's an off-by-one question: does "one cycle before DMA end" mean:
- The read happens at cycle 160, and DMA transitions at END of cycle 160? OR
- The read happens at cycle 160, and DMA transitions at END of cycle 161?

### Issue 5: Condition Evaluation Timing

Going back to the Jump implementation, there's another subtle issue. The condition is evaluated BETWEEN reading the low byte and reading the high byte. But on actual hardware, when does condition evaluation happen?

According to the test documentation:
```
; JP cc, nn is expected to have the following timing:
; M = 0: instruction decoding
; M = 1: nn read: memory access for low byte
; M = 2: nn read: memory access for high byte
; M = 3: internal delay
```

This suggests that M-cycles 1 and 2 should be purely memory reads, with no condition evaluation between them. The condition evaluation might happen AFTER both bytes are read (during M-cycle 2 or 3).

**Current implementation**: Evaluates condition between the two reads.
**Expected behavior**: Should read both bytes consecutively, then evaluate condition.

This could cause a timing issue where the second read doesn't happen at the expected cycle.

## Summary

The most likely failure reasons are:

1. **Condition evaluation happens between byte reads**: The current implementation evaluates `evaluateCondition()` between reading the low and high bytes. This might introduce timing issues if it affects when the second read occurs. The condition should probably be evaluated AFTER both bytes are read.

2. **OAM DMA timing off-by-one**: The exact cycle when memory blocking ends might be off by one cycle. The test is extremely precise about when the memory read happens relative to DMA completion.

3. **Clock tick ordering**: The memory read happens BEFORE the clock tick in `readIndirectPCAndIncrement`, which means the read sees the DMA state from the previous cycle. This needs careful verification against actual hardware behavior.

The emulator should be tested with detailed logging to see exactly which cycle the high byte read occurs and what the DMA phase is at that moment.
