# Mooneye Test Analysis: call_cc_timing

## Test Overview

The `call_cc_timing` test verifies the precise timing behavior of the `CALL cc, nn` (conditional CALL) instruction on the Game Boy. Specifically, it tests when memory reads occur during the execution of the CALL instruction when the condition is true, using OAM DMA's memory access restrictions to determine the exact machine cycle when the high byte of the target address is read. This is similar to `call_timing` but specifically tests the conditional variant `CALL C, nn`.

## What The Test Does

### Setup Phase

1. **Memory Configuration**: The test sets up a specific memory layout:
   - Waits for VBLANK
   - Copies the rest of `wram_test` (starting at offset +2) to VRAM at address `$8000` (16 bytes)
   - Copies `wram_test` to OAM, starting at address `$FDFE` (OAM - 2) (16 bytes)
   - The `wram_test` contains a `CALL C, $1A00` instruction (opcode `$DC`) that will be positioned so:
     - Byte 0 (opcode `$DC`) is at `$FDFE`
     - Byte 1 (low address `$00`) is at `$FDFF` (OAM - 1)
     - Byte 2 (high address) is at `$FE00` (the first byte of OAM)

2. **HIRAM Setup**: Copies test procedure (`hiram_test`) to HIRAM (`$FF80-$FFE0`) so code can execute during OAM DMA when other memory is blocked.

### Round 1: Testing Memory Read Timing During OAM DMA

1. **Preparation** (in `hiram_test` at `$FF80`):
   - Sets the low byte of the CALL target address to `$CA` at OAM - 1 (`$FDFF`)
   - Starts OAM DMA from source address `$8000` (copying from VRAM to OAM)
   - Uses precise timing delays: 38 DEC/JR loop iterations + 2 NOPs
   - Loads HL with `$FDFE` (OAM - 2)
   - Sets carry flag (SCF) to make the condition true
   - Jumps to HL (`$FDFE`) where the CALL C instruction begins

2. **Execution Timing**:
   - The CALL C instruction executes with this timing (according to test comments):
     - M=0: Instruction decoding
     - M=1: Read low byte of nn (from `$FDFF`)
     - M=2: Read high byte of nn (from `$FE00`, first byte of OAM)
     - M=3: Internal delay
     - M=4: Push PC high byte to stack
     - M=5: Push PC low byte to stack

3. **The Critical Test**:
   - The timing is carefully aligned so the M=2 memory read (high byte from `$FE00`) happens **exactly one cycle before OAM DMA completes**
   - During DMA, CPU cannot read from OAM, so it reads `$FF` instead of the actual value
   - At this point in the test, OAM hasn't been fully written yet, but even if it had been, DMA blocks access
   - Therefore, the CALL becomes `CALL C, $FFCA` instead of `CALL C, $1ACA`
   - Since carry flag is set, the condition is true and it should jump to `finish_round1` at `$FFCA`
   - `finish_round1` contains 2 NOPs and then jumps to `test_round2`

4. **Failure Condition**: If timing is wrong, it jumps to `$1ACA` which leads to `fail_round1` ("FAIL: ROUND 1").

### Round 2: Testing Memory Read After OAM DMA Ends

1. **Preparation** (in `test_round2` at `$FF80 + offset`):
   - Sets the low byte of the CALL target address to `$DA` at OAM - 1 (`$FDFF`)
   - Starts OAM DMA from source address `$8000` again
   - Uses slightly different timing: 38 DEC/JR loop iterations + **3 NOPs** (one more than round 1)
   - Loads HL with `$FDFE` (OAM - 2)
   - Sets carry flag (SCF) to make the condition true
   - Jumps to HL (`$FDFE`) where the CALL C instruction begins

2. **Execution Timing**:
   - The extra NOP delays execution by one machine cycle
   - Now the M=2 memory read (high byte from `$FE00`) happens **exactly after OAM DMA completes**
   - After DMA, CPU can read from OAM normally
   - At this point, OAM has been written with data from `$8000` (VRAM), which contains the copied `wram_test` code
   - The high byte at `$FE00` (first byte of OAM) now contains `$1A` (from the original CALL C, $1A00 instruction)
   - Therefore, the CALL becomes `CALL C, $1ADA` instead of `CALL C, $FFDA`
   - Since carry flag is set, the condition is true and it should jump to `finish_round2` at `$1ADA`
   - `finish_round2` contains 2 NOPs and then jumps to `test_finish` which calls `quit_ok`

3. **Failure Condition**: If timing is wrong, it jumps to `$FFDA` which leads to `fail_round2` ("FAIL: ROUND 2").

## What The Test Expects

The test expects the following precise timing behavior for the `CALL C, nn` instruction when the carry flag is set (condition is true):

1. **Machine Cycle Breakdown**:
   - **M=0**: Instruction fetch/decode (handled by CPU fetch cycle)
   - **M=1**: Memory read for low byte of target address (1 M-cycle)
   - **M=2**: Memory read for high byte of target address (1 M-cycle)
   - **M=3**: Internal delay (1 M-cycle with no memory access)
   - **M=4**: Memory write for high byte of PC to stack (1 M-cycle)
   - **M=5**: Memory write for low byte of PC to stack (1 M-cycle)
   - **Total**: 6 M-cycles = 24 T-cycles (when condition is true)

2. **Expected Success Path**:
   - Round 1: CALL C reads `$FF` as high byte during DMA → jumps to `$FFCA` → executes `finish_round1` → continues to round 2
   - Round 2: CALL C reads `$1A` as high byte after DMA → jumps to `$1ADA` → executes `finish_round2` → test completes with success message
   - Final output: "Test OK" message via serial port

3. **Expected Failure Path**:
   - If timing is off by even one cycle, the test will print "FAIL: ROUND 1" or "FAIL: ROUND 2"

## What The Test Is Testing

This test validates several critical aspects of the Game Boy CPU and DMA implementation:

1. **CALL cc Instruction Timing**: The exact sequence and duration of memory operations during conditional CALL execution when the condition is true
2. **Internal Delay Cycle**: The presence of M=3 internal delay cycle between reading the target address and pushing PC to stack
3. **Condition Evaluation Timing**: That the condition is evaluated before committing to the full CALL operation, but the memory reads for the target address happen regardless
4. **DMA Memory Access Blocking**: That OAM DMA properly blocks CPU access to memory regions outside HRAM/IO registers
5. **DMA Duration**: That OAM DMA takes exactly 160 T-cycles (40 M-cycles) as specified
6. **Memory Read Order**: That the CALL instruction reads low byte first, then high byte (little-endian)

## Potential Failure Reasons

### 1. Missing Internal Delay Cycle (MOST LIKELY)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Call.java`

**Current Implementation**:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short functionPointer = ControlFlow.readImm16(cpuStructure);

    // technically, the if statement should be in the previous clock tick.
    if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
        ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());

        cpuStructure.registers().setPC(functionPointer);
    }
}
```

**Analysis**:
- The `readImm16()` method consumes M=1 and M=2 (reading low and high bytes)
- The condition is evaluated (but this should happen during M=3 based on the comment in the code)
- The `pushToStack()` method handles M=4 and M=5 (writing to stack)
- **MISSING**: There is no M=3 internal delay cycle between reading the address and pushing to stack
- According to the test comments, the CALL cc instruction should have:
  - M=0: instruction decoding
  - M=1: nn read (low byte)
  - M=2: nn read (high byte)
  - M=3: internal delay ← **THIS IS MISSING**
  - M=4: PC push (high byte)
  - M=5: PC push (low byte)

**Expected Fix**:
After `readImm16()` and before evaluating the condition/pushing to stack, there should be a `cpuStructure.clock().tick()` to implement the M=3 internal delay cycle. The condition evaluation should logically happen during this internal delay.

### 2. Incorrect pushToStack Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

**Current Implementation**:
```java
public static void pushToStack(CpuStructure cpuStructure, short value) {
    decrementSP(cpuStructure);

    cpuStructure.clock().tick();

    cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));
    decrementSP(cpuStructure);

    cpuStructure.clock().tick();

    cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));

    cpuStructure.clock().tick();
}
```

**Analysis**:
- The current implementation takes 3 clock ticks for the push operation
- According to the test specification, the push should take exactly 2 M-cycles:
  - M=4: Write high byte to stack (includes SP decrement)
  - M=5: Write low byte to stack (includes SP decrement)
- The current implementation has 3 ticks total, which suggests there may be an extra tick or the ticks are in the wrong places
- Looking at the code flow:
  1. Decrement SP
  2. Tick (M-cycle)
  3. Write high byte
  4. Decrement SP
  5. Tick (M-cycle)
  6. Write low byte
  7. Tick (M-cycle)

**Expected Behavior**:
According to Game Boy timing documentation, stack operations work as follows:
- Each memory write consumes 1 M-cycle
- The SP decrements happen as part of the memory write cycle, not as separate cycles
- Therefore, pushToStack should only have 2 ticks total, one for each byte written

However, this may not be the primary issue since the test is specifically testing the timing of the address read (M=2), which happens before the push. The push timing would affect later operations but not the critical test condition.

### 3. Condition Evaluation Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Call.java`

**Current Implementation**:
```java
short functionPointer = ControlFlow.readImm16(cpuStructure);

// technically, the if statement should be in the previous clock tick.
if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
    ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());
    cpuStructure.registers().setPC(functionPointer);
}
```

**Analysis**:
- The comment says "technically, the if statement should be in the previous clock tick"
- This suggests the developer is aware that the condition evaluation timing might not be correct
- In the actual Game Boy, the condition evaluation happens during the M=3 internal delay cycle
- The current implementation evaluates the condition after M=2 but doesn't consume a cycle for it
- When the condition is false, CALL cc takes only 3 M-cycles (M=0, M=1, M=2), suggesting the condition is checked after reading the immediate value
- When the condition is true, it takes 6 M-cycles total (M=0 through M=5)

**Expected Behavior**:
The condition should be evaluated during the M=3 internal delay cycle. This internal delay always happens when the condition is true, regardless of when the evaluation occurs logically. The key is that there's a 1 M-cycle gap between reading the address and starting the push operation.

### 4. DMA Timing Edge Cases

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

**Current Implementation**:
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

**Potential Issue**:
The test relies on precise timing of when DMA ends and memory becomes accessible again. The implementation has:
- REQUESTED phase (cycle when DMA register is written)
- PENDING phase (1 cycle delay before first transfer)
- TRANSFERRING phase (160 bytes = 160 T-cycles = 40 M-cycles)

The transition from TRANSFERRING to INACTIVE happens when `dmaByteIndex >= OAM_SIZE` (160).

**Analysis of DMA Timing**:
1. Cycle N: Write to DMA register → REQUESTED
2. Cycle N+1: PENDING (preparation phase)
3. Cycle N+2 to N+161: TRANSFERRING (160 byte transfers)
4. Cycle N+162: INACTIVE

The test needs to ensure that:
- In round 1, the M=2 read happens during the last cycle of TRANSFERRING (or just before DMA completes)
- In round 2, the M=2 read happens after DMA transitions to INACTIVE (one cycle later)

If there's a one-cycle discrepancy in when memory blocking ends, the test will fail.

### 5. Memory Access During DMA Transitions

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

**Current Implementation**:
```java
private boolean isBlocking() {
    return switch (dmaPhase) {
        case INACTIVE -> false;
        case REQUESTED, PENDING -> blockingDuringSetup;
        case TRANSFERRING -> true;
    };
}
```

**Analysis**:
The test carefully aligns memory reads to happen exactly at the DMA boundary:
- Round 1: Read should happen while DMA is still TRANSFERRING → returns `0xFF`
- Round 2: Read should happen after DMA becomes INACTIVE → returns actual memory value

**Timing Details**:
When `mCycle()` is called and `dmaByteIndex` reaches 160, the phase transitions to INACTIVE. The question is: within that same M-cycle, if a memory read happens after the phase transition, does it see INACTIVE or TRANSFERRING?

The order of operations within an M-cycle matters:
1. If DMA phase transition happens at the start of the M-cycle, and the memory read happens later in the same M-cycle, the read would see INACTIVE
2. If DMA phase transition happens at the end of the M-cycle, and the memory read happens earlier in the same M-cycle, the read would see TRANSFERRING

The test timing with 38 DEC/JR loops + 2 NOPs vs 3 NOPs creates exactly a 1 M-cycle difference, so the implementation needs to have precise cycle-accurate timing.

### 6. Opcode Cycle Count Verification

**Expected Cycles** (from Opcodes.json):
- CALL C, nn (0xDC): 24 T-cycles (6 M-cycles) when condition is true, 12 T-cycles (3 M-cycles) when false

**Current Implementation Analysis**:
Let's trace through the current implementation when condition is true:
1. M=0: Instruction fetch (handled by CPU.fetch_cycle)
2. M=1: Read low byte in `readIndirectPCAndIncrement()` - includes `clock().tick()`
3. M=2: Read high byte in `readIndirectPCAndIncrement()` - includes `clock().tick()`
4. Evaluate condition (no tick)
5. Call `pushToStack()`:
   - Tick 1 (after first SP decrement)
   - Write high byte
   - Tick 2 (after second SP decrement)
   - Write low byte
   - Tick 3

**Total**: M=0 + M=1 + M=2 + 3 ticks in push = 6 M-cycles ✓

However, the structure is wrong:
- Should be: M=0, M=1, M=2, M=3 (internal delay), M=4 (push high), M=5 (push low)
- Currently is: M=0, M=1, M=2, M=3 (push tick 1), M=4 (push tick 2), M=5 (push tick 3)

The missing internal delay between M=2 and the push operation means the memory read timing is off by one cycle.

## Summary

The most likely cause of failure is the **missing internal delay cycle (M=3)** in the CALL cc instruction implementation. The current implementation immediately proceeds to evaluate the condition and push the PC to the stack after reading the 16-bit immediate value, but the actual Game Boy hardware has a 1 M-cycle internal delay between reading the address and starting the push operation.

This timing difference would cause the test's carefully aligned memory reads to occur at the wrong cycles relative to the DMA end boundary, resulting in incorrect address values being read and incorrect jump targets.

**The specific issue**:
- Without the M=3 internal delay, all subsequent operations (including the fetch of the next instruction) happen one cycle earlier than expected
- This shifts the timing of when round 1 and round 2's address reads occur relative to the DMA completion
- In round 1, the read might happen after DMA completes instead of before
- In round 2, the read might happen before DMA completes instead of after
- This would cause the test to jump to the wrong addresses and trigger the failure conditions

**The fix would require**:
1. Adding a single `cpuStructure.clock().tick()` call in the Call.java execute method after reading the immediate value and before checking the condition/pushing to stack
2. Verifying that the pushToStack timing matches the 2 M-cycle specification (M=4 and M=5) with proper cycle accounting
3. Ensuring DMA timing transitions are cycle-accurate, particularly around the TRANSFERRING → INACTIVE transition
