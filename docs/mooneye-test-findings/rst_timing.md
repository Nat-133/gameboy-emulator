# RST Timing Test Analysis

## Test Overview

The `rst_timing` test verifies the precise cycle-by-cycle timing of the RST (restart) instruction. Specifically, it tests when memory accesses occur during the RST instruction's 4-cycle execution, focusing on the interaction between RST and OAM DMA memory blocking.

## What The Test Does

### Initial Setup
1. Disables interrupts (`di`)
2. Waits for VBLANK
3. Fills the first $20 bytes of VRAM (starting at $8000) with value $81
4. Copies the test code to HRAM ($FF80) and jumps there (necessary because OAM DMA blocks most memory access)

### Round 1: Testing M=3 (Low Byte Push)
1. Sets stack pointer to `OAM+$10` ($FE10)
2. Starts OAM DMA from $8000 (the VRAM we just filled with $81)
3. Waits precisely 38 iterations (dec/jr loop) to align timing
4. Loads HL with address of `finish_round1` in HRAM
5. Executes 2 NOPs for timing alignment
6. **Executes `RST $38`**
   - At address $38, there's a `JP HL` instruction
   - RST pushes PC to stack during cycles M=2 (high byte) and M=3 (low byte)
   - The test is timed so that OAM becomes accessible exactly at M=3
   - Expected result: high byte should be $81 (corrupted by OAM DMA), low byte should be correct
7. After RST, execution jumps to HL (finish_round1)
8. Executes 2 NOPs
9. Pops BC from stack (retrieving the corrupted PC value)

### Round 2: Testing M=2 (High Byte Push)
1. Starts another OAM DMA from $8000
2. Waits 38 iterations again
3. Loads HL with address of `finish_round2` in HRAM
4. Executes **3 NOPs** this time (one more than round 1)
5. **Executes `RST $38`**
   - The extra NOP shifts timing by 1 cycle
   - Now OAM becomes accessible exactly at M=2
   - Expected result: both high and low bytes should be correct (not corrupted)
6. After RST, jumps to finish_round2
7. Executes 2 NOPs
8. Pops DE from stack
9. Jumps to test_finish

### Test Verification
At `test_finish`, the test checks:
- `assert_b $81` - High byte from round 1 should be $81 (corrupted)
- `assert_c $9E` - Low byte from round 1 should be $9E (correct)
- `assert_d $FF` - High byte from round 2 should be $FF (correct)
- `assert_e $BD` - Low byte from round 2 should be $BD (correct)

## What The Test Expects

The test expects these specific register values at completion:
- **B = $81**: This proves that at M=3 of RST (when pushing low byte of PC), the previous write at M=2 (pushing high byte) was corrupted by OAM DMA reading $81 from VRAM
- **C = $9E**: The actual correct low byte value of PC
- **D = $FF**: The correct high byte of PC when OAM is accessible at M=2
- **E = $BD**: The correct low byte of PC

These values prove that:
1. RST's memory write for the high byte occurs at M=2
2. RST's memory write for the low byte occurs at M=3
3. Memory blocking during OAM DMA can corrupt writes at specific cycles

## What The Test Is Testing

This test validates:

1. **RST Instruction Timing**: The exact 4-cycle breakdown of RST:
   - M=0: Instruction decoding
   - M=1: Internal delay
   - M=2: Push PC high byte to stack (memory write)
   - M=3: Push PC low byte to stack (memory write)

2. **OAM DMA Memory Blocking**: During OAM DMA:
   - Memory outside HRAM ($FF80-$FFFE) and I/O registers ($FF00-$FF7F) is blocked
   - When memory becomes accessible during an instruction, it should affect only the memory access happening at that exact cycle

3. **Stack Push Operation Timing**: Specifically tests when the two bytes of a 16-bit stack push happen

## Potential Failure Reasons

### 1. Incorrect RST Timing Implementation

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Restart.java`

The current implementation:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());
    cpuStructure.registers().setPC(address);
}
```

**Problem**: This implementation delegates to `pushToStack` which has its own timing, but there's no explicit M=0 (decode) or M=1 (internal delay) cycle handling visible in the RST instruction itself.

### 2. pushToStack Timing Issues

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

Current implementation:
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

**Timing Analysis**:
- Tick 1: After first SP decrement (should be M=1 internal delay)
- First write: high byte - happens after tick 1 (would be M=2)
- Tick 2: After second SP decrement
- Second write: low byte - happens after tick 2 (would be M=3)
- Tick 3: Final tick

**Problems**:
1. Total is 3 ticks in pushToStack, plus 1 from fetch cycle = 4 M-cycles total (correct)
2. However, the timing breakdown may not match the expected behavior:
   - The test expects M=2 to do the memory write for high byte
   - The test expects M=3 to do the memory write for low byte
3. The memory writes appear to happen at the right cycles (after tick 1 and tick 2), but the fetch cycle tick might shift everything

### 3. Fetch Cycle Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/Cpu.java`

```java
private void fetch_cycle(Instruction instruction) {
    if (!instruction.handlesFetch()) {
        fetch();
        cpuStructure.clock().tick();  // M=0 decode happens here

        handlePotentialInterrupt();

        instruction.postFetch(cpuStructure);
    }
}
```

**Problem**: The fetch/decode cycle happens AFTER the instruction executes, but RST's M=0 should be part of its 4-cycle timing. This suggests:
- The instruction's execution covers M=1, M=2, M=3
- The fetch cycle covers the NEXT instruction's M=0

This is actually correct for most instructions, but RST needs to account for its own M=0 as part of its 4 cycles.

### 4. Critical Timing Issue: When Do Memory Writes Actually Happen?

Looking at the test expectations:
- Round 1: OAM accessible at M=3, expects high byte corrupted → means high byte write happened BEFORE M=3
- Round 2: OAM accessible at M=2, expects both bytes correct → means high byte write happens AT OR AFTER M=2

This is contradictory unless understood as:
- In Round 1: The high byte was written when OAM was still blocking (before M=3), so it reads $FF (blocked value), but the test expects $81 which means it read from the DMA source during the write
- In Round 2: The high byte write happens at M=2 when OAM becomes accessible, so it writes correctly

**The Real Issue**: The emulator needs to handle OAM DMA's memory blocking timing precisely:
- During DMA blocking, reads return $FF
- But the test expects $81, which is the value being DMA'd from VRAM
- This suggests the memory write during RST is somehow reading the value being transferred by DMA

### 5. OAM DMA Memory Access During RST

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

The DMA blocking implementation:
```java
@Override
public byte read(short address) {
    ensureDmaListenerRegistered();
    if (isBlocking() && !isAccessibleDuringDma(address)) {
        return (byte) 0xFF;
    }
    return underlying.read(address);
}
```

**Problem**: The test expects the corrupted high byte to be $81 (the value being DMA'd from VRAM to OAM). But the current implementation returns $FF when blocked.

**Expected Behavior**: When CPU writes to the stack during RST while OAM DMA is active and that stack address is being written by DMA at the same cycle, the value should be what DMA is writing, not $FF.

This suggests that OAM DMA writes and CPU writes can collide, and when they do, the DMA value wins.

### 6. Summary of Likely Issues

1. **OAM DMA collision behavior**: When RST writes to stack at the same address/time that OAM DMA is writing, the DMA value should be written instead of the CPU's value. The current implementation doesn't model this collision behavior.

2. **Precise cycle alignment**: The test is extremely timing-sensitive. The memory write operations in RST must happen at exactly M=2 and M=3, and these must align with the OAM DMA's transfer cycles.

3. **Missing M=0/M=1 accounting**: The RST instruction needs explicit handling of its decode (M=0) and internal delay (M=1) phases to ensure M=2 and M=3 are correctly positioned for the memory writes.

4. **DMA phase transitions**: The test relies on OAM becoming accessible at precise moments. The DMA state machine transitions (REQUESTED → PENDING → TRANSFERRING) need to align correctly with instruction cycle boundaries.
