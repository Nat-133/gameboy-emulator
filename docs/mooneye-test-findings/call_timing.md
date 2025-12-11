# Mooneye Test Analysis: call_timing

## Test Overview

The `call_timing` test verifies the precise timing behavior of the `CALL nn` instruction on the Game Boy. Specifically, it tests when memory reads occur during the execution of the CALL instruction by exploiting OAM DMA's memory access restrictions to determine the exact machine cycle when the high byte of the target address is read.

## What The Test Does

### Setup Phase

1. **Memory Configuration**: The test sets up a specific memory layout:
   - Copies code from `wram_test` to VRAM starting at address `$8000`
   - Copies code from `wram_test` to OAM, starting at address `$FDFE` (OAM - 2)
   - The `wram_test` contains a `CALL $1A00` instruction that will be positioned so its first byte (opcode `$CD`) is at `$FDFE`, second byte (low address `$00`) is at `$FDFF`, and third byte (high address) is at `$FE00` (the first byte of OAM)

2. **HIRAM Setup**: Copies test procedure (`hiram_test`) to HIRAM (`$FF80-$FFE0`) so code can execute during OAM DMA when other memory is blocked.

### Round 1: Testing Memory Read Timing During OAM DMA

1. **Preparation**:
   - Sets the low byte of the CALL target address to `$CA` at OAM - 1 (`$FDFF`)
   - Starts OAM DMA from source address `$8000`
   - Uses precise timing delays (38 DEC/JR loop cycles + 3 NOPs)

2. **Execution Timing**:
   - Jumps to `$FDFE` where the CALL instruction begins
   - The CALL instruction executes with this timing:
     - M=0: Instruction decoding
     - M=1: Read low byte of nn (from `$FDFF`) = `$CA`
     - M=2: Read high byte of nn (from `$FE00`, first byte of OAM)
     - M=3: Internal delay
     - M=4: Push PC high byte to stack
     - M=5: Push PC low byte to stack

3. **The Critical Test**:
   - The timing is carefully aligned so the M=2 memory read (high byte from `$FE00`) happens **exactly one cycle before OAM DMA completes**
   - During DMA, CPU cannot read from OAM, so it reads `$FF` instead of `$1A`
   - Therefore, the CALL becomes `CALL $FFCA` instead of `CALL $1ACA`
   - This should jump to `finish_round1` at `$FFCA`, which continues to round 2

4. **Failure Condition**: If timing is wrong, it jumps to `$1ACA` which leads to `fail_round1`.

### Round 2: Testing Memory Read After OAM DMA Ends

1. **Preparation**:
   - Sets the low byte of the CALL target address to `$DA` at OAM - 1 (`$FDFF`)
   - Starts OAM DMA from source address `$8000` again
   - Uses slightly different timing (38 DEC/JR loop cycles + **4 NOPs** instead of 3)

2. **Execution Timing**:
   - The extra NOP delays execution by one machine cycle
   - Now the M=2 memory read (high byte from `$FE00`) happens **exactly after OAM DMA completes**
   - After DMA, CPU can read from OAM normally, so it reads `$1A` (from the copied data)
   - Therefore, the CALL becomes `CALL $1ADA` instead of `CALL $FFDA`
   - This should jump to `finish_round2` at `$1ADA`, which completes the test successfully

3. **Failure Condition**: If timing is wrong, it jumps to `$FFDA` which leads to `fail_round2`.

## What The Test Expects

The test expects the following precise timing behavior for the `CALL nn` instruction:

1. **Machine Cycle Breakdown**:
   - **M=0**: Instruction fetch/decode (handled by CPU fetch cycle)
   - **M=1**: Memory read for low byte of target address (1 M-cycle)
   - **M=2**: Memory read for high byte of target address (1 M-cycle)
   - **M=3**: Internal delay (1 M-cycle with no memory access)
   - **M=4**: Memory write for high byte of PC to stack (1 M-cycle)
   - **M=5**: Memory write for low byte of PC to stack (1 M-cycle)

2. **Expected Success Path**:
   - Round 1: CALL reads `$FF` as high byte during DMA → jumps to `$FFCA` → prints nothing → continues to round 2
   - Round 2: CALL reads `$1A` as high byte after DMA → jumps to `$1ADA` → test completes with success
   - Final output: Success indicator (B=3, C=5, D=8, E=13, H=21, L=34)

3. **Expected Failure Path**:
   - If timing is off by even one cycle, the test will print "FAIL: ROUND 1" or "FAIL: ROUND 2"

## What The Test Is Testing

This test validates several critical aspects of the Game Boy CPU and DMA implementation:

1. **CALL Instruction Timing**: The exact sequence and duration of memory operations during CALL execution
2. **Internal Delay Cycle**: The presence of M=3 internal delay cycle between reading the target address and pushing PC to stack
3. **DMA Memory Access Blocking**: That OAM DMA properly blocks CPU access to memory regions outside HRAM/IO registers
4. **DMA Duration**: That OAM DMA takes exactly 160 cycles (40 M-cycles) as specified
5. **Memory Read Order**: That the CALL instruction reads low byte first, then high byte (little-endian)

## Potential Failure Reasons

### 1. Missing Internal Delay Cycle (MOST LIKELY)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Call.java`

**Current Implementation**:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    short functionPointer = ControlFlow.readImm16(cpuStructure);

    if (ControlFlow.evaluateCondition(condition, cpuStructure.registers())) {
        ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());
        cpuStructure.registers().setPC(functionPointer);
    }
}
```

**Analysis**:
- The `readImm16()` method consumes M=1 and M=2 (reading low and high bytes)
- The `pushToStack()` method handles M=4 and M=5 (writing to stack)
- **MISSING**: There is no M=3 internal delay cycle between reading the address and pushing to stack
- According to the test comments, the CALL instruction should have:
  - M=0: instruction decoding
  - M=1: nn read (low byte)
  - M=2: nn read (high byte)
  - M=3: internal delay ← **THIS IS MISSING**
  - M=4: PC push (high byte)
  - M=5: PC push (low byte)

**Expected Fix**:
After `readImm16()` and before `pushToStack()`, there should be a `cpuStructure.clock().tick()` to implement the M=3 internal delay cycle.

### 2. Incorrect pushToStack Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

**Current Implementation**:
```java
public static void pushToStack(CpuStructure cpuStructure, short value) {
    decrementSP(cpuStructure);
    cpuStructure.clock().tick();  // Tick 1

    cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));
    decrementSP(cpuStructure);
    cpuStructure.clock().tick();  // Tick 2

    cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));
    cpuStructure.clock().tick();  // Tick 3
}
```

**Analysis**:
- The current implementation takes 3 clock ticks for the push operation
- According to the test specification, the push should take exactly 2 M-cycles:
  - M=4: Write high byte to stack
  - M=5: Write low byte to stack
- The current implementation appears to have an extra tick or the ticks might be in the wrong places

**Expected Behavior**:
The push operation should align with the test's expectation of 2 M-cycles for the stack push (M=4 and M=5).

### 3. DMA Timing Edge Cases

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
            // Transfer logic
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
- PENDING phase (1 cycle delay)
- TRANSFERRING phase (160 bytes = 160 cycles)

The transition from TRANSFERRING to INACTIVE happens when `dmaByteIndex >= OAM_SIZE` (160). The test needs to ensure that memory becomes accessible at exactly the right cycle. If there's a one-cycle discrepancy in when memory blocking ends, the test will fail.

### 4. Memory Access During DMA Transitions

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

**Potential Issue**:
The test carefully aligns memory reads to happen exactly at the DMA boundary. If the memory read in round 1 happens during the last transfer cycle (when `dmaByteIndex == 159`), but the DMA is still in TRANSFERRING state, it should return `0xFF`. In round 2, if the read happens after DMA transitions to INACTIVE (when `dmaByteIndex == 160` and phase becomes INACTIVE), it should return the actual value from memory.

The timing of when `dmaPhase` transitions from TRANSFERRING to INACTIVE within the M-cycle could cause off-by-one errors.

## Summary

The most likely cause of failure is the **missing internal delay cycle (M=3)** in the CALL instruction implementation. The current implementation immediately proceeds to push the PC to the stack after reading the 16-bit immediate value, but the actual Game Boy hardware has a 1 M-cycle internal delay between these operations. This timing difference would cause the test's carefully aligned memory reads to occur at the wrong cycles relative to the DMA end boundary, resulting in incorrect address values being read and incorrect jump targets.

The fix would require adding a single `cpuStructure.clock().tick()` call in the Call.java execute method after reading the immediate value and before checking the condition/pushing to stack. Additionally, the pushToStack timing may need to be reviewed to ensure it matches the 2 M-cycle specification (M=4 and M=5) rather than 3 cycles.
