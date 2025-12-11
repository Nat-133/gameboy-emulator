# RET Timing Test Analysis

## Test Overview

The `ret_timing` test validates the precise cycle-by-cycle timing of the RET (Return) instruction. It uses OAM DMA memory blocking to create precise timing windows, testing whether RET executes in exactly 4 M-cycles with the correct memory access pattern for each cycle. The test verifies that memory accesses happen only during M-cycles 1-2, and that M-cycle 3 is an internal delay with no memory access.

## What The Test Does

### Setup Phase

1. **Copy callback to HIRAM ($FF80-$FF81)**:
   ```assembly
   hiram_cb:
     xor a      ; Clear A register (A = 0)
     jp hl      ; Jump to address in HL
   ```
   - This 2-byte sequence can execute during OAM DMA since HIRAM ($FF80-$FFFE) remains accessible
   - Located at $FF80: opcode $AF (xor a), followed by jp hl opcodes

2. **Prepare ROM code at $2080**:
   ```assembly
   .org $2080
   ld a, $01   ; Set A = 1
   jp hl       ; Jump to address in HL
   ```
   - This code will execute if PC jumps to $2080

### Test Round 1: Verify memory access during M-cycles 1-2

**Goal**: Confirm that RET's M-cycles 1-2 access memory (and can be blocked by DMA), while M-cycle 3 does not.

**Setup**:
```assembly
wait_vblank
ld hl, OAM - 1      ; HL = $FDFF
ld a, $80
ld (hl), a           ; Memory[$FDFF] = $80

ld hl, VRAM         ; HL = $8000
ld a, $20
ld (hl), a           ; Memory[$8000] = $20

ld SP, OAM-1        ; SP = $FDFF (stack points here)
ld hl, finish_round1 ; HL = address of finish_round1 label
```

After this setup:
- Stack pointer SP = $FDFF
- Memory[$FDFF] = $80 (will be popped as low byte of PC)
- Memory[$FE00] = OAM start (initially contains whatever was there)
- Memory[$8000] = $20 (DMA source data - will be copied to $FE00)
- HL = finish_round1 (jump target for code at $FF80 and $2080)

**Timing synchronization**:
```assembly
ld b, 39
start_oam_dma $80   ; Start DMA from $8000 to $FE00
- dec b             ; Loop: 39 iterations
  jr nz, -          ; Each iteration: 3 M-cycles (dec=1, jr nz=2 when taken)
nops 3              ; 3 more M-cycles

ret                 ; Execute RET
```

**DMA timing analysis**:

The `start_oam_dma $80` macro expands to:
```assembly
wait_vblank
ld a, $80
ldh (<DMA), a       ; Write to $FF46 register
```

Delay calculation:
- After DMA write to $FF46, the delay loop executes
- Delay loop: 39 × 3 = 117 M-cycles (dec b = 1 M-cycle, jr nz = 2 M-cycles when taken)
- 3 NOPs: 3 M-cycles
- Total delay: 120 M-cycles from DMA start
- RET begins at M-cycle 121

**DMA phases** (based on Game Boy DMA behavior):
- M-cycle 0: Write to $FF46, DMA state becomes REQUESTED
- M-cycle 1: DMA transitions to PENDING (preparation phase)
- M-cycle 2: DMA transitions to TRANSFERRING, byte 0 copied ($8000 → $FE00)
- M-cycles 2-161: TRANSFERRING phase (160 bytes transferred, 1 byte per M-cycle)
- M-cycle 162+: INACTIVE

During TRANSFERRING phase (M-cycles 2-161):
- Memory regions blocked: All except HIRAM ($FF80-$FFFE) and I/O registers ($FF00-$FF7F)
- Reads from blocked regions return $FF
- Memory[$FE00] = $20 after M-cycle 2 (copied from $8000)

**RET execution timing**:

RET starts at cycle 121 (relative to DMA start):
- M-cycle 121: RET M=0 (instruction decode/fetch, already happened)
- M-cycle 122: RET M=1 (pop low byte from [$FDFF])
- M-cycle 123: RET M=2 (pop high byte from [$FE00])
- M-cycle 124: RET M=3 (internal delay)

At M-cycles 122-123, DMA is still in TRANSFERRING phase (transferring bytes 120-121 of 160).

**Memory access behavior during DMA**:

At M-cycle 122 (RET M=1):
- Reading $FDFF: This is in echo RAM region ($E000-$FDFF), blocked during DMA
- However, based on the test's expected behavior, this read must succeed
- This suggests either:
  - The blocking hasn't fully activated yet, OR
  - There's a 1-cycle offset in when memory reads check the DMA state

At M-cycle 123 (RET M=2):
- Reading $FE00: This is OAM start, should be blocked during DMA
- Returns $FF when blocked

**Expected PC value calculation**:

The test expects PC = $FF80 (high byte $FF, low byte $80), which means:
- Low byte read at M-cycle 1: succeeds, reads $80 from [$FDFF]
- High byte read at M-cycle 2: blocked, reads $FF from [$FE00]

This creates PC = concat($FF, $80) = $FF80

**Execution flow after RET**:

When PC = $FF80:
1. CPU fetches next instruction from $FF80 (HIRAM, accessible during DMA)
2. Reads $AF (xor a opcode)
3. Executes xor a → A = 0
4. Fetches next instruction: jp hl
5. Jumps to finish_round1
6. At finish_round1: `or a; jr z, test_round2`
7. Since A = 0, zero flag is set, branches to test_round2 ✓ **PASS**

If PC were $2080 instead:
1. CPU tries to fetch from $2080 (ROM, blocked during DMA)
2. Reads $FF (blocked), interpreted as RST $38
3. Wrong execution path
4. Eventually A ≠ 0 at finish_round1
5. `or a; jr z, test_round2` fails to branch ✗ **FAIL**

### Test Round 2: Verify timing precision with +1 cycle offset

**Goal**: Confirm that with one extra M-cycle delay, both bytes can be read successfully.

**Setup**:
```assembly
wait_vblank
ld hl, OAM          ; HL = $FE00
ld a, $FF
ld (hl), a           ; Memory[$FE00] = $FF (will be overwritten by DMA)

ld SP, OAM-1        ; SP = $FDFF
ld hl, finish_round2

ld b, 39
start_oam_dma $80
- dec b
  jr nz, -
nops 4              ; 4 NOPs instead of 3 - ONE extra M-cycle!

ret
```

**Key difference**: Round 2 does NOT write to $FDFF, and uses 4 NOPs instead of 3.

Stack contents:
- Memory[$FDFF] = $80 (still there from Round 1, or possibly $00)
- Memory[$FE00] = $FF initially, then $20 after DMA copies from $8000

**Timing**:
- DMA start: M-cycle 0
- Delay: 117 + 4 = 121 M-cycles
- RET begins at M-cycle 122 (one cycle later than round 1)

**RET execution**:
- M-cycle 122: RET M=0 (instruction decode)
- M-cycle 123: RET M=1 (pop low byte from [$FDFF])
- M-cycle 124: RET M=2 (pop high byte from [$FE00])
- M-cycle 125: RET M=3 (internal delay)

**Expected behavior**:

With the extra 1-cycle delay, the timing shifts such that:
- M-cycle 123 (RET M=1): Still during DMA blocking, BUT might be at edge where it succeeds
- M-cycle 124 (RET M=2): Possibly past the critical blocking window for the first few bytes

The test expects PC = $2080:
- Low byte: $80 (from [$FDFF])
- High byte: $20 (from [$FE00], either read successfully or DMA has copied $20 there)

When PC = $2080:
1. CPU fetches from $2080
2. If still during DMA, this returns $FF (RST $38) - would fail
3. If DMA blocking has ended or shifted, reads actual opcode (ld a, $01)
4. Executes ld a, $01 → A = 1
5. Executes jp hl → jumps to finish_round2
6. At finish_round2: `or a; jr nz, test_success`
7. Since A = 1, zero flag is clear, branches to test_success ✓ **PASS**

**Critical insight**: The single M-cycle difference must cause a shift in which memory addresses are blocked at which exact moments during RET execution.

## What The Test Expects

Based on the test logic:

**Round 1** (`or a; jr z, test_round2`):
- Expects A = 0 to pass
- A = 0 means HIRAM callback at $FF80 was executed (`xor a`)
- This happens when PC = $FF80
- Requires: low byte read succeeds ($80), high byte read returns $FF (blocked)

**Round 2** (`or a; jr nz, test_success`):
- Expects A ≠ 0 (specifically A = 1) to pass
- A = 1 means ROM code at $2080 was executed (`ld a, $01`)
- This happens when PC = $2080
- Requires: both bytes read successfully, creating PC = $2080

**Summary of expected values**:
- Round 1 (120-cycle delay): PC after RET = $FF80
- Round 2 (121-cycle delay): PC after RET = $2080

## What The Test Is Testing

The test validates the Game Boy CPU's RET instruction timing at M-cycle granularity:

1. **RET instruction M-cycle breakdown**:
   - M-cycle 0: Instruction decode (fetch already happened in previous instruction's final cycle)
   - M-cycle 1: Memory read of low byte from stack (SP), increment SP
   - M-cycle 2: Memory read of high byte from stack (SP), increment SP
   - M-cycle 3: Internal delay with no memory access

2. **Memory access timing**:
   - M-cycles 1-2 must perform actual memory reads that can be blocked by DMA
   - The exact timing of these reads relative to other operations matters

3. **M-cycle 3 is internal**:
   - M-cycle 3 must NOT access memory
   - Cannot be affected by DMA blocking

4. **Precise sub-cycle timing**:
   - A single M-cycle difference (120 vs 121 cycles) must produce measurably different behavior
   - Memory blocking must be checked at the exact right moment relative to reads

5. **DMA memory blocking behavior**:
   - During OAM DMA, most memory returns $FF
   - HIRAM ($FF80-$FFFE) remains accessible
   - I/O registers ($FF00-$FF7F) remain accessible
   - Blocking window must be precisely timed

## Potential Failure Reasons

### 1. Incorrect RET M-Cycle Count

**Current Implementation** (`Return.java`):
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short value = ControlFlow.popFromStack(cpuStructure);
    cpuStructure.registers().setPC(value);
    cpuStructure.clock().tick();
}
```

**ControlFlow.popFromStack**:
```java
public static short popFromStack(CpuStructure cpuStructure) {
    byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
    incrementSP(cpuStructure);
    cpuStructure.clock().tick();  // M-cycle 1

    byte msb = cpuStructure.memory().read(cpuStructure.registers().SP());
    incrementSP(cpuStructure);
    cpuStructure.clock().tick();  // M-cycle 2

    return concat(msb, lsb);
}
```

**M-cycle count**:
- M-cycle 0: Instruction decode (handled by CPU.cycle() fetch phase)
- M-cycle 1: Read LSB + tick ✓
- M-cycle 2: Read MSB + tick ✓
- M-cycle 3: Final tick in execute() ✓
- **Total: 4 M-cycles ✓**

The M-cycle count appears correct.

### 2. Memory Read Timing Within M-Cycle

**Potential issue**: The order of operations within an M-cycle is critical.

Current order in popFromStack:
1. `memory.read(address)` - performs the read
2. `incrementSP()` - updates stack pointer
3. `clock.tick()` - advances the clock

**Problem**: If DMA blocking state is checked during the memory read, but the DMA state machine is updated during `clock.tick()`, there could be a timing mismatch.

**What should happen**:
- The memory read should check the DMA blocking state that exists at the START of the M-cycle
- The DMA state machine should update AFTER the memory read completes
- This ensures reads and blocking state changes are properly sequenced

**Current behavior might be wrong if**:
- DMA state updates happen before memory reads check the blocking state
- This would cause reads to see the "future" blocking state instead of the current one

### 3. DMA Blocking State Transition Timing

The test requires extremely precise DMA blocking timing. The current `MemoryBus.java` implementation has:

```java
private enum DmaPhase {
    INACTIVE,
    REQUESTED,    // Same cycle as write to $FF46
    PENDING,      // Delay before transfers begin
    TRANSFERRING
}

@Override
public void mCycle() {
    switch (dmaPhase) {
        case INACTIVE -> {}
        case REQUESTED -> dmaPhase = DmaPhase.PENDING;
        case PENDING -> dmaPhase = DmaPhase.TRANSFERRING;
        case TRANSFERRING -> {
            // ... transfer logic ...
        }
    }
}

private boolean isBlocking() {
    return switch (dmaPhase) {
        case INACTIVE -> false;
        case REQUESTED, PENDING -> blockingDuringSetup;
        case TRANSFERRING -> true;
    };
}
```

**Potential issues**:

1. **Phase transition timing**: When does `mCycle()` get called relative to instruction execution?
   - If it's called before memory reads, blocking state is ahead by 1 cycle
   - If it's called after memory reads, blocking state is behind by 1 cycle

2. **Blocking start timing**: The transition from PENDING to TRANSFERRING happens at a specific cycle
   - If this happens too early, Round 1 low byte would be blocked (wrong)
   - If this happens too late, Round 2 high byte wouldn't be blocked (wrong)

3. **Off-by-one in cycle counting**: The test expects:
   - Cycle 122: Low byte read succeeds in Round 1
   - Cycle 123: High byte read blocked in Round 1
   - Cycle 123-124: Both reads succeed in Round 2
   - This requires precise synchronization

### 4. DMA Transfer Count

Current implementation:
```java
dmaByteIndex++;
if (dmaByteIndex >= OAM_SIZE) {
    dmaPhase = DmaPhase.INACTIVE;
}
```

**Question**: Does OAM_SIZE = 160 (0xA0)?

DMA should transfer exactly 160 bytes, taking 160 M-cycles in TRANSFERRING phase. If the count is wrong, the blocking window ends too early or too late.

### 5. Clock Synchronization Between CPU and DMA

**Critical requirement**: The CPU and DMA must share the same clock and be perfectly synchronized.

Current architecture:
- CPU has a clock that ticks during instruction execution
- DMA has `mCycle()` method that's called to advance DMA state
- These must be called in the correct order

**Question**: When is `MemoryBus.mCycle()` called?
- Is it called once per CPU M-cycle?
- Is it called before or after instruction execution?
- Is it called before or after memory reads?

If `mCycle()` is called at the wrong time relative to instruction execution:
- The DMA state will be ahead or behind by 1 cycle
- Memory blocking checks will see the wrong DMA state
- The test will fail

### 6. Instruction Fetch After RET

After RET completes and PC is set, the CPU must fetch the next instruction:

```java
// In CPU.cycle()
void cycle() {
    Instruction instruction = decode(...);
    instruction.execute(cpuStructure);  // RET executes here, sets PC
    fetch_cycle(instruction);           // Fetch next instruction
}

private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();                        // Reads from new PC location
        cpuStructure.clock().tick();
        // ...
    }
}
```

**Timing consideration**:
- After RET finishes at cycle 124 (Round 1), PC = $FF80
- The fetch happens at cycle 125
- At cycle 125, DMA is still in TRANSFERRING phase
- Fetch from $FF80 (HIRAM) succeeds ✓
- Fetch from $2080 (ROM) would return $FF ✗

For Round 2:
- RET finishes at cycle 125, PC = $2080
- Fetch happens at cycle 126
- At cycle 126, DMA might still be TRANSFERRING
- Fetch from $2080 (ROM) might return $FF ✗

**This is a problem!** Unless:
- The DMA blocking has ended by cycle 126, OR
- The memory at $FE00 already contains $20 (from DMA), so reading it during RET M=2 succeeds

**Alternative understanding**: Maybe the high byte read in Round 2 actually succeeds because:
- Memory[$FE00] was overwritten to $20 by DMA before RET reads it
- The blocking check passes because the address is in OAM which gets special treatment
- Or the timing is such that the read happens just after DMA finishes that byte

### 7. Stack Pointer Increment Timing

The current implementation increments SP before the clock ticks:
```java
byte lsb = cpuStructure.memory().read(cpuStructure.registers().SP());
incrementSP(cpuStructure);  // SP changes immediately
cpuStructure.clock().tick();
```

**Hardware behavior**: On real hardware, SP increment likely happens during the same M-cycle as the read, but the exact sub-cycle timing matters.

If DMA or other hardware checks the SP value, the timing of when SP changes could matter. However, this is unlikely to affect the test since DMA doesn't depend on SP.

### 8. Memory Region Blocking Rules

Current implementation:
```java
private boolean isAccessibleDuringDma(short address) {
    int addr = address & 0xFFFF;
    return (addr >= 0xFF00 && addr <= 0xFFFE);
}
```

This allows access to $FF00-$FFFE during DMA. Let's verify this is correct:
- $FF00-$FF7F: I/O registers ✓
- $FF80-$FFFE: HIRAM ✓
- $FFFF: Interrupt Enable register (part of I/O?) ✓

**Potential issue**: Is $FFFF accessible? The code says yes (up to $FFFE), but the comment mentions "$FF00-$FFFE". The IE register at $FFFF should probably be accessible.

### 9. The Critical Timing Question

The test's expected behavior suggests a very specific timing:

**Round 1 (120 cycles delay)**:
- RET M=1 at cycle 122: Read succeeds ($80)
- RET M=2 at cycle 123: Read blocked ($FF)

**Round 2 (121 cycles delay)**:
- RET M=1 at cycle 123: Read succeeds ($80)
- RET M=2 at cycle 124: Read succeeds ($20)

This suggests that:
- Cycle 122: Blocking NOT active
- Cycle 123: Blocking IS active
- Cycle 124: Blocking NOT active (or $FE00 specifically accessible)

**But wait**: If DMA TRANSFERRING starts at cycle 2 and lasts 160 cycles, it should be active from cycle 2-161. Both 122-124 are within this range!

**This means**:
- Either the blocking doesn't work as simply as "blocked during TRANSFERRING"
- Or there's a specific cycle offset in when reads check the blocking state
- Or the DMA state machine timing is different than expected
- Or memory writes by DMA to $FE00 happen early enough that reading $FE00 gets $20

### 10. Possible Resolution: Memory Write by DMA

**Alternative theory**:

At cycle 2, DMA copies byte 0 from $8000 to $FE00:
- Memory[$FE00] = $20

In Round 1, RET M=2 happens at cycle 123:
- Reads from $FE00
- Gets... $20? No, that doesn't match expected behavior

Actually, if memory is "blocked", it means the read returns $FF regardless of what's actually stored there. So even though Memory[$FE00] = $20, reading it during DMA returns $FF.

This still doesn't resolve the timing paradox.

### 11. Most Likely Issue: DMA mCycle() Call Timing

**Hypothesis**: The `MemoryBus.mCycle()` method is not being called at all, or is being called at the wrong frequency.

If DMA state never advances:
- DMA stays in REQUESTED or PENDING
- Blocking never activates (unless `blockingDuringSetup` is true)
- All reads succeed
- Both rounds would produce PC = $2080
- Round 1 would fail (expects PC = $FF80)

**What to check**:
- Is `MemoryBus.mCycle()` being called once per CPU M-cycle?
- Who is responsible for calling it?
- Is it being called at the right time relative to instruction execution?

### 12. DmaController Interface

The `MemoryBus` implements `DmaController`:
```java
public interface DmaController {
    void startDma(byte sourceHigh);
    void mCycle();
    boolean isDmaActive();
}
```

**Question**: Where is `mCycle()` called from?

Looking at the project structure, there should be some central clock or timing coordinator that calls `mCycle()` once per M-cycle. If this isn't happening, DMA won't function correctly.

## Summary

The `ret_timing` test is a sophisticated cycle-accurate timing test that validates:
- RET takes exactly 4 M-cycles
- M-cycles 1-2 perform memory reads that can be blocked by DMA
- M-cycle 3 is an internal delay with no memory access
- Precise timing of memory reads relative to DMA blocking state

**Expected behavior**:
- Round 1: RET reads $80 and $FF, creating PC = $FF80, leading to A = 0
- Round 2: RET reads $80 and $20, creating PC = $2080, leading to A = 1

**Most likely failure causes** (in order of probability):

1. **DMA mCycle() not being called**: If the DMA state machine isn't advancing, blocking won't work
   - Check if `MemoryBus.mCycle()` is being called once per CPU M-cycle
   - Verify the calling timing relative to instruction execution

2. **DMA state transition timing**: DMA phases might transition at wrong cycles
   - Verify REQUESTED → PENDING → TRANSFERRING happens at correct cycles
   - Check that blocking activates/deactivates at the right time

3. **Memory read vs DMA state check ordering**: Reads might check DMA state at wrong time
   - Ensure memory reads check DMA state at START of M-cycle
   - Ensure DMA state updates happen AFTER reads complete

4. **Clock synchronization**: CPU and DMA cycle counters might be misaligned
   - Verify both use same clock source
   - Verify perfect 1:1 M-cycle correspondence

5. **Instruction fetch timing**: The fetch after RET might fail if DMA still blocking
   - For Round 1: Fetch from $FF80 should succeed (HIRAM)
   - For Round 2: Fetch from $2080 might fail if DMA still blocking at that cycle

**Recommended debugging approach**:
1. Add logging to track DMA phase at each M-cycle
2. Log memory read results during RET execution
3. Log final PC value after RET
4. Log A register value at finish_round1 and finish_round2
5. Compare actual vs expected values to identify the timing mismatch
