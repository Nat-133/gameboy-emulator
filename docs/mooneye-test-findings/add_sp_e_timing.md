# Mooneye Test Analysis: add_sp_e_timing

## Test Overview

The `add_sp_e_timing` test verifies the exact M-cycle timing of the `ADD SP, e` instruction (opcode 0xE8) by exploiting OAM DMA memory blocking behavior. The test executes the instruction at precisely timed moments during OAM DMA transfers to verify that the memory read for the signed offset parameter `e` happens at exactly M-cycle 1 (the second M-cycle of the instruction, after opcode decoding).

## What The Test Does

### Setup Phase

1. **Wait for VBlank**: Ensures stable timing by waiting for LY=144.

2. **Prepare Code in Special Memory Locations**:
   - The first byte of the `wram_test` procedure (opcode 0xE8 for `ADD SP, e`) is copied to `$FDFF` (OAM - 1)
   - The rest of the `wram_test` procedure is copied to VRAM starting at `$8000`
   - This means the 2-byte instruction `ADD SP, $42` spans addresses `$FDFF` and `$FE00`
   - Address `$FE00` is the first byte of OAM memory

3. **Initialize Stack Pointer**: Sets SP to `$FFFE`.

4. **Copy Test to HIRAM**: Copies the test procedure to HIRAM (`$FF80+`) so it can execute during OAM DMA (when most memory is blocked).

### Test Round 1: Memory Read During DMA (Expected: SP = $FFFD)

**Goal**: Verify the `e` parameter is read one cycle before OAM DMA ends, so DMA blocks the read and returns `$FF`.

```assembly
test_round1:
  ld b, 38
  start_oam_dma $80    ; Start DMA from $8000 (fills OAM with $42)
- dec b
  jr nz, -             ; Loop 38 times
  nops 1               ; One NOP for fine-tuning
  ld de, $FF80 + (finish_round1 - hiram_test)
  ld hl, OAM - 1       ; HL = $FDFF
  jp hl                ; Jump to $FDFF where ADD SP,e instruction starts
```

**Timing intention**: The test carefully times execution so that:
- OAM DMA is started from source address `$8000` (which contains the value `$42`)
- The 38 loops + 1 NOP create a precise delay
- When `ADD SP, e` executes at `$FDFF-$FE00`, the read of the `e` parameter at `$FE00` happens exactly **one cycle before OAM DMA completes**
- Since DMA is still active, the read is blocked and returns `$FF` instead of `$42`
- The instruction executes as `ADD SP, -1` (since `$FF` as signed byte = -1)
- **Expected result**: SP = `$FFFE - 1 = $FFFD`

The result is saved via:
```assembly
ld hl, sp+$00        ; Load SP into HL
ld a, h
ld (result_tmp), a   ; Save high byte
ld a, l
ld (result_tmp+1), a ; Save low byte
```

Then jumps to `finish_round1` which stores the result to `result_round1`.

### Test Round 2: Memory Read After DMA (Expected: SP = $0040)

**Goal**: Verify the `e` parameter is read exactly when OAM DMA completes, so the read succeeds and gets `$42`.

```assembly
test_round2:
  ld b, 38
  start_oam_dma $80    ; Start DMA from $8000 again
- dec b
  jr nz, -             ; Loop 38 times (same as round 1)
  nops 2               ; TWO NOPs (one more than round 1)
  ld de, $FF80 + (finish_round2 - hiram_test)
  ld hl, OAM - 1       ; HL = $FDFF
  jp hl                ; Jump to $FDFF where ADD SP,e instruction starts
```

**Timing intention**: With one additional NOP:
- The read of the `e` parameter at `$FE00` happens exactly **when OAM DMA completes**
- DMA is no longer blocking, so the read succeeds and returns `$42`
- The instruction executes as `ADD SP, $42`
- **Expected result**: SP = `$FFFE + $42 = $0040`

### Assertion Phase

The test verifies the results:
```assembly
assert_b $FF         ; High byte of Round 1: $FFFD >> 8 = $FF
assert_c $FD         ; Low byte of Round 1: $FFFD & $FF = $FD
assert_d $00         ; High byte of Round 2: $0040 >> 8 = $00
assert_e $40         ; Low byte of Round 2: $0040 & $FF = $40
```

## What The Test Expects

After both rounds complete:
- **BC register** = `$FFFD` (result from Round 1)
  - B = `$FF`
  - C = `$FD`
- **DE register** = `$0040` (result from Round 2)
  - D = `$00`
  - E = `$40`

## What The Test Is Testing

This test validates the **precise M-cycle timing of the ADD SP, e instruction**, specifically:

### Expected Timing (4 M-cycles total):
- **M = 0**: Instruction decoding (opcode fetch at PC, which is $FDFF)
- **M = 1**: Memory access for the signed offset `e` (read from PC+1, which is $FE00)
- **M = 2**: Internal delay (ALU performs lower byte addition)
- **M = 3**: Internal delay (ALU performs upper byte adjustment)

### Key Behavior Being Validated:

1. **Memory Read Timing**: The `e` parameter must be read at exactly M-cycle 1 (not M-cycle 0, not M-cycle 2).

2. **OAM DMA Blocking**: During OAM DMA transfer, memory reads return `$FF` for non-HIRAM addresses. The test exploits this to detect when the read actually occurs.

3. **Instruction Duration**: The full instruction must take exactly 4 M-cycles (16 T-cycles).

### How The Test Validates Timing:

By placing the `e` parameter at the first byte of OAM (`$FE00`) and controlling exactly when OAM DMA completes:
- If the read happens too early (during DMA), it gets `$FF` → SP becomes `$FFFD`
- If the read happens at the right time (after DMA completes), it gets `$42` → SP becomes `$0040`
- The one NOP difference between rounds creates exactly one M-cycle difference in timing
- This proves the memory read happens at a specific, deterministic M-cycle

## Potential Failure Reasons

### 1. Incorrect Memory Read Timing in ADD SP,e Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Add.java`

**Current Implementation**:
```java
private void executeSignedAddition(CpuStructure cpuStructure) {
    OperationTargetAccessor operationTargetAccessor = OperationTargetAccessor.from(cpuStructure);
    short leftValue = operationTargetAccessor.getValue(this.left);  // SP
    byte rightValue = (byte) operationTargetAccessor.getValue(this.right);  // IMM_8

    short res = ControlFlow.signedAdditionOnlyAlu(leftValue, rightValue, cpuStructure);
    operationTargetAccessor.setValue(this.left, res);
}
```

**Issue**: The `getValue(this.right)` call reads the immediate byte from memory:

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/OperationTargetAccessor.java`

```java
case IMM_8 -> ControlFlow.readIndirectPCAndIncrement(cpuStructure);
```

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

```java
public static byte readIndirectPCAndIncrement(CpuStructure cpuStructure) {
    byte value = cpuStructure.memory().read(cpuStructure.registers().PC());
    incrementPC(cpuStructure);
    cpuStructure.clock().tick();  // ← Clock tick happens AFTER the read
    return value;
}
```

**Problem**: The memory read happens, then PC is incremented, then the clock ticks. However, the clock tick is what advances DMA state (via `dmaController.mCycle()`). This means:
- The read happens at the **beginning** of M-cycle 1
- The DMA state advances at the **end** of M-cycle 1 (when clock.tick() is called)

In the test scenario:
- Round 1: DMA should still be blocking when the read happens → should return `$FF`
- Round 2: DMA should have just finished when the read happens → should return `$42`

The critical question is: **Does the memory read see the DMA state from before or after the mCycle() call?**

### 2. DMA State Machine Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

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
                dmaPhase = DmaPhase.INACTIVE;  // ← DMA completes here
            }
        }
    }
}
```

**Issue**: OAM DMA takes 160 M-cycles to transfer 160 bytes (OAM_SIZE = 0xA0 = 160). On the 160th M-cycle:
1. The last byte is transferred
2. `dmaByteIndex` becomes 160
3. DMA phase becomes `INACTIVE`

**Problem with Clock Integration**:

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/CpuModule.java`

```java
return new ClockWithParallelProcess(() -> {
    timer.mCycle();
    serialController.mCycle();
    dmaController.mCycle();  // ← DMA state advances here
    for (int i = 0; i < 4; i++) {
        ppu.tCycle();
    }
});
```

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/ClockWithParallelProcess.java`

```java
@Override
public void tick() {
    parallelProcess.run();  // ← All hardware updates happen here
    time++;
}
```

**The Order of Operations**:
1. CPU instruction calls `cpuStructure.memory().read(address)`
2. Memory read checks `isBlocking()` which looks at current DMA phase
3. Later in the same instruction, `cpuStructure.clock().tick()` is called
4. This triggers `dmaController.mCycle()` which updates DMA state
5. But the read already happened using the old state!

### 3. Potential Timing Issue Analysis

**Expected Behavior** (according to test):
- At the end of M-cycle 159 (the 160th cycle, 0-indexed): Last DMA byte transfers, DMA becomes INACTIVE
- At the start of M-cycle 160: OAM is now readable
- If `ADD SP, e` reads its parameter during M-cycle 160, it should read the actual value from OAM

**Possible Implementation Issue**:
The memory read in `readIndirectPCAndIncrement` happens **before** `clock.tick()` is called. This means:
- The read sees the DMA state from the **current** M-cycle
- The `clock.tick()` then advances to the **next** M-cycle
- But the read should see the DMA state that is active **during** the M-cycle when the read occurs

**Timeline for Round 1** (one NOP):
```
M-cycle 159: Last DMA transfer, dmaPhase = TRANSFERRING → INACTIVE
             This happens during a previous instruction's clock.tick()
M-cycle 160: ADD SP,e opcode fetch (PC=$FDFF)
             This is handled by the previous instruction's fetch cycle
M-cycle 161: ADD SP,e executes, reads e parameter (PC=$FE00)
             DMA is INACTIVE, so read should succeed → gets $42
```

But the test expects Round 1 to read `$FF` (blocked by DMA), which means the read should happen when DMA is still active.

**Timeline for Round 2** (two NOPs):
```
M-cycle 160: Last DMA transfer, dmaPhase = TRANSFERRING → INACTIVE
M-cycle 161: ADD SP,e opcode fetch (PC=$FDFF)
M-cycle 162: ADD SP,e executes, reads e parameter (PC=$FE00)
             DMA is INACTIVE, so read should succeed → gets $42
```

### 4. Instruction Fetch Cycle Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java`

```java
public void cycle() {
    Instruction instruction = decode(cpuStructure.registers().instructionRegister());
    instruction.execute(cpuStructure);
    fetch_cycle(instruction);
}

private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();  // ← M-cycle 0 of NEXT instruction
        handlePotentialInterrupt();
        instruction.postFetch(cpuStructure);
    }
}

private void fetch() {
    short pc = cpuStructure.registers().PC();
    cpuStructure.registers().setPC(cpuStructure.idu().increment(pc));
    this.cpuStructure.registers().setInstructionRegister(cpuStructure.memory().read(pc));
}
```

**Issue**: The opcode fetch for the NEXT instruction happens at the end of the CURRENT instruction. This is correct for Game Boy timing, but it means:
- When `ADD SP, e` at `$FDFF` starts executing, the opcode has already been fetched in the previous instruction's `fetch_cycle()`
- M-cycle 0 of `ADD SP, e` is actually the fetch cycle that happened at the end of the `JP HL` instruction
- M-cycle 1 is when `ADD SP, e` reads the `e` parameter

### 5. The Core Problem: When Does Memory Read See DMA State?

The fundamental issue is: **When a memory read happens within an M-cycle, does it see the DMA state from before or after that M-cycle's DMA transfer?**

**Current Implementation**: Memory reads happen BEFORE `clock.tick()` is called, which means they see the DMA state from BEFORE the M-cycle's DMA transfer.

**Real Hardware**: The memory read happens DURING the M-cycle, and DMA blocking is checked in real-time as the read occurs.

**Potential Fix Direction**: The DMA phase transition (specifically TRANSFERRING → INACTIVE on the 160th byte) might need to happen at a different point in the M-cycle, or memory reads need to be checked against DMA state differently.

### 6. Expected vs Actual Timing

**Expected timing for the test**:
```
start_oam_dma $80: Cycle 0 (REQUESTED)
                   Cycle 1 (PENDING)
                   Cycles 2-161 (TRANSFERRING, transfers bytes 0-159)
                   Cycle 162+ (INACTIVE)
```

**Round 1** (1 NOP): `ADD SP,e` memory read should happen at cycle 161 (last transfer cycle)
- DMA still TRANSFERRING → read blocked → returns $FF

**Round 2** (2 NOPs): `ADD SP,e` memory read should happen at cycle 162 (first INACTIVE cycle)
- DMA INACTIVE → read succeeds → returns $42

**Potential Bug**: The emulator might have the DMA transitions off by one cycle, or the memory read might be seeing the DMA state from the wrong phase of the M-cycle.

### 7. Cycle Counting Precision

The test uses very precise cycle counting:
- 38 loops of `dec b; jr nz` = 38 * (1 + 3) = 152 M-cycles (when branch taken) + 1 * (1 + 2) = 155 total
- `start_oam_dma` includes `wait_vblank` + `ld a, $80` + `ldh ($FF46), a` = variable + 2 + 3 = variable + 5
- Each NOP = 1 M-cycle
- `ld de, imm16` = 3 M-cycles
- `ld hl, imm16` = 3 M-cycles
- `jp hl` = 1 M-cycle

The exact cycle when `ADD SP,e` reads its parameter depends on all these timings being precisely correct in the emulator.

## Summary

The `add_sp_e_timing` test validates that the `ADD SP, e` instruction:
1. Takes exactly 4 M-cycles total
2. Reads the signed offset `e` parameter at exactly M-cycle 1
3. Performs internal ALU operations at M-cycles 2 and 3

The most likely failure reasons in the emulator are:
1. **DMA state visibility**: Memory reads may see DMA state from before the M-cycle's DMA operation completes, causing off-by-one timing errors
2. **DMA phase transitions**: The transition from TRANSFERRING to INACTIVE may happen at the wrong sub-cycle boundary
3. **Clock tick ordering**: The order of memory read → clock tick → DMA update may not match hardware behavior
4. **Cycle counting**: Subtle differences in other instruction timings could shift when the critical memory read occurs relative to DMA completion
