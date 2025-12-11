# Mooneye Test Analysis: push_timing

## Test Overview

The `push_timing` test verifies the precise timing behavior of the `PUSH rr` instruction on the Game Boy. Specifically, it tests when memory writes occur during the execution of the PUSH instruction by exploiting OAM DMA's memory access restrictions to determine the exact machine cycles when the high byte and low byte are written to the stack.

## What The Test Does

### Setup Phase

1. **Memory Configuration**: The test sets up the OAM memory region:
   - Waits for VBLANK to ensure safe VRAM access
   - Sets the first $20 (32) bytes of VRAM starting at address `$8000` to `$81`
   - This provides known values when the test reads back data from OAM during DMA

2. **HIRAM Setup**: Copies the test procedure (`hiram_test`) to HIRAM (`$FF80-$FFDF`) so code can execute during OAM DMA when other memory is blocked. The test code must run from HIRAM because during OAM DMA, the CPU cannot access any memory except HIRAM ($FF80-$FFFE) and I/O registers ($FF00-$FF7F).

### Test Round 1: Testing PUSH at M=2 (High Byte Write)

1. **Preparation**:
   - Sets SP to `OAM+$10` (`$FE10`), pointing into the middle of OAM
   - Loads register D with `$42` and register E with `$24` (DE = `$4224`)
   - Starts OAM DMA from source address `$8000` (VRAM)
   - Uses precise timing delays: 39 iterations of DEC/JR loop + 2 NOPs

2. **Execution Timing**:
   - The timing delay calculation:
     - `ld a, 39` = 2 cycles (8 t-states)
     - Loop: `dec a` (1 cycle) + `jr nz, -` (3 cycles) = 4 cycles per iteration
     - 39 iterations × 4 cycles = 156 cycles
     - Final `dec a` + `jr nz` not taken = 2 cycles
     - Total loop: 2 + 156 + 2 = 160 cycles (40 M-cycles) to wait
     - 2 NOPs = 2 cycles
   - After the delay, OAM DMA has completed 42 transfers
   - Executes `PUSH DE`

3. **The Critical Test (M=2)**:
   - The timing is aligned so that when `PUSH DE` writes the **high byte** (at M=2), OAM is accessible because DMA hasn't blocked it yet at that specific point
   - Expected PUSH timing:
     - M=0: Instruction decoding
     - M=1: Internal delay
     - **M=2: Write high byte of DE ($42) to [SP-1] = $FE0F**
     - M=3: Write low byte of DE ($24) to [SP-2] = $FE0E
   - At M=2, if the implementation is correct, the write to $FE0F should succeed and write `$42`
   - Then 7 NOPs are executed (7 cycles)
   - Then `POP HL` retrieves the value: HL should be `$4224`

4. **What's Being Verified**:
   - After the POP, H should be `$42` (high byte was successfully written at M=2)
   - After the POP, L should be `$24` (low byte was successfully written at M=3)

### Test Round 2: Testing PUSH at M=3 (Low Byte Write)

1. **Preparation**:
   - DE is still `$4224` from before
   - Starts another OAM DMA from source address `$8000`
   - Uses slightly different timing: 39 iterations of DEC/JR loop + **1 NOP** (instead of 2)

2. **Execution Timing**:
   - The timing delay calculation:
     - Loop: 160 cycles (40 M-cycles) as before
     - 1 NOP = 1 cycle
   - This is exactly 1 cycle earlier than round 1
   - Executes `PUSH DE`

3. **The Critical Test (M=3)**:
   - The timing is aligned so that when `PUSH DE` writes the **low byte** (at M=3), OAM is accessible
   - At M=2 (high byte write), OAM might be blocked by DMA or the write might be at the transition point
   - At M=3 (low byte write), OAM should definitely be accessible
   - Expected behavior:
     - M=2: Write high byte to $FE0F (might read from VRAM source $8000 = `$81`)
     - M=3: Write low byte to $FE0E (should succeed with `$24`)

4. **What's Being Verified**:
   - After 7 NOPs and `POP DE`, the test checks:
     - D should be `$81` (either the DMA transferred VRAM data, or timing allows reading the original DMA source)
     - E should be `$24` (low byte was successfully written at M=3)

### Final Assertions

The test expects:
- **H = `$42`**: High byte from round 1 (OAM accessible at M=2)
- **L = `$24`**: Low byte from round 1 (OAM accessible at M=3)
- **D = `$81`**: High byte from round 2 (DMA source value or timing-dependent)
- **E = `$24`**: Low byte from round 2 (OAM accessible at M=3)

## What The Test Expects

The test expects the following precise timing behavior for the `PUSH rr` instruction:

1. **Machine Cycle Breakdown**:
   - **M=0**: Instruction fetch/decode (handled by CPU fetch cycle)
   - **M=1**: Internal delay (1 M-cycle with no memory access)
   - **M=2**: Memory write for **high byte** to [SP-1] (1 M-cycle)
   - **M=3**: Memory write for **low byte** to [SP-2] (1 M-cycle)

2. **Memory Write Order**:
   - PUSH writes the **high byte first** (at M=2), then the low byte (at M=3)
   - SP is decremented before each write
   - Final SP = original SP - 2

3. **Expected Success**:
   - Round 1: High byte write at M=2 succeeds → H=`$42`, L=`$24`
   - Round 2: Low byte write at M=3 succeeds → D=`$81`, E=`$24`
   - All four register values match expected values

## What The Test Is Testing

This test validates several critical aspects of the Game Boy CPU and DMA implementation:

1. **PUSH Instruction Timing**: The exact sequence and duration of memory operations during PUSH execution
2. **Internal Delay Cycle**: The presence of M=1 internal delay cycle before writing to stack
3. **Memory Write Order**: That PUSH writes high byte first (at M=2), then low byte (at M=3)
4. **Stack Pointer Decrementation**: That SP is decremented before each write, not after
5. **DMA Memory Access Timing**: The precise cycle-by-cycle behavior of OAM accessibility during DMA transfers
6. **OAM Write Accessibility**: When writes to OAM succeed or are blocked during DMA

## Potential Failure Reasons

### 1. Missing Internal Delay Cycle (MOST LIKELY)

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

**Current Implementation**:
```java
public static void pushToStack(CpuStructure cpuStructure, short value) {
    decrementSP(cpuStructure);      // SP--

    cpuStructure.clock().tick();     // 1 cycle

    cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));
    decrementSP(cpuStructure);       // SP--

    cpuStructure.clock().tick();     // 1 cycle

    cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));

    cpuStructure.clock().tick();     // 1 cycle
}
```

**Analysis**:
The current implementation has the following timing:
- Decrement SP (no clock tick)
- M=1: Clock tick (internal delay) - **CORRECT**
- Write high byte (no immediate clock tick)
- Decrement SP (no clock tick)
- M=2: Clock tick - **WRONG: should be after high byte write**
- Write low byte (no immediate clock tick)
- M=3: Clock tick - **WRONG: should be after low byte write**

**The Problem**:
The clock ticks are happening **between** SP decrements and memory writes, rather than **after** each memory write. According to the test expectations:
- M=1: Internal delay (correct)
- M=2: High byte write to memory (the write should occur during M=2, with clock tick after)
- M=3: Low byte write to memory (the write should occur during M=3, with clock tick after)

The correct timing should be:
```
M=0: Instruction decode (handled by CPU)
M=1: Internal delay (tick)
M=2: Decrement SP, write high byte to [SP], tick
M=3: Decrement SP, write low byte to [SP], tick
```

**Expected Fix Pattern** (based on other timing tests):
```java
public static void pushToStack(CpuStructure cpuStructure, short value) {
    // M=1: Internal delay
    cpuStructure.clock().tick();

    // M=2: Write high byte
    decrementSP(cpuStructure);
    cpuStructure.memory().write(cpuStructure.registers().SP(), upper_byte(value));
    cpuStructure.clock().tick();

    // M=3: Write low byte
    decrementSP(cpuStructure);
    cpuStructure.memory().write(cpuStructure.registers().SP(), lower_byte(value));
    cpuStructure.clock().tick();
}
```

### 2. DMA Timing Granularity

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

**Analysis**:
The DMA implementation operates at M-cycle granularity (one transfer per M-cycle). The `isBlocking()` method determines when memory is blocked:

```java
private boolean isBlocking() {
    return switch (dmaPhase) {
        case INACTIVE -> false;
        case REQUESTED, PENDING -> blockingDuringSetup;
        case TRANSFERRING -> true;
    };
}
```

**The Problem**:
The test is checking for very precise sub-M-cycle timing behavior. Specifically:
- At M=2 of PUSH (high byte write): The test expects OAM to be accessible at a specific point
- At M=3 of PUSH (low byte write): The test expects OAM to be accessible at a specific point

However, the DMA controller operates at M-cycle granularity and blocks **the entire M-cycle** when in TRANSFERRING phase. This means:
- If DMA is transferring, ALL memory accesses within that M-cycle are blocked
- There's no sub-M-cycle granularity for when DMA reads source vs writes destination

**Expected Behavior**:
Real Game Boy hardware has sub-M-cycle timing where:
- DMA transfers happen in specific phases within each M-cycle
- There may be specific windows when OAM is accessible even during active DMA
- The blocking behavior might not be uniform throughout the entire M-cycle

**Potential Issue**:
The test carefully times the PUSH instruction to align with specific DMA transfer cycles. If the emulator's DMA blocking is too coarse-grained (blocking the entire M-cycle uniformly), it might not match the real hardware behavior where there are specific sub-cycle windows of accessibility.

### 3. Stack Pointer Decrementation Timing

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/common/ControlFlow.java`

**Current Implementation**:
```java
public static void decrementSP(CpuStructure cpuStructure) {
    cpuStructure.registers().setSP(cpuStructure.idu().decrement(cpuStructure.registers().SP()));
}
```

**Analysis**:
The SP is decremented using the IDU (increment/decrement unit), which is correct. However, the timing of when SP is decremented relative to the memory write matters.

**Current Sequence**:
1. Decrement SP
2. Tick clock
3. Write to [SP]
4. Decrement SP
5. Tick clock
6. Write to [SP]
7. Tick clock

**Expected Sequence** (based on test expectations):
1. Tick clock (internal delay)
2. Decrement SP, write to [SP], tick clock
3. Decrement SP, write to [SP], tick clock

The key difference is that memory writes should happen during the same M-cycle as the clock tick, not after separate ticks.

### 4. Memory Write During DMA

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

**Current Implementation**:
```java
@Override
public void write(short address, byte value) {
    ensureDmaListenerRegistered();
    underlying.write(address, value);
}
```

**Analysis**:
The `write` method doesn't check if DMA is blocking. Unlike `read`, which returns `0xFF` if DMA is blocking, `write` always proceeds.

**The Problem**:
This might be correct for the actual Game Boy behavior (writes might not be blocked the same way reads are), but it could also be an issue if:
- Writes to OAM during DMA should be blocked or corrupted
- The test expects certain write behavior that depends on DMA state

**Expected Behavior**:
The test suggests that writes to OAM during certain phases of DMA should either:
- Succeed and write the value
- Write to the DMA source instead (bus conflict)
- Be ignored/corrupted

The current implementation doesn't model any special write behavior during DMA.

### 5. Clock Tick Ordering in PUSH

**Location**: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/cpu/instructions/Push.java` and `ControlFlow.java`

**Analysis**:
The fundamental issue is that the clock ticks are not aligned with the memory operations correctly. The test expects:

```
M=0: Fetch (handled by CPU.cycle())
M=1: Internal delay - TICK
M=2: Write high byte - TICK
M=3: Write low byte - TICK
```

But the current implementation does:
```
M=0: Fetch (handled by CPU.cycle())
     Decrement SP
M=1: TICK
     Write high byte
     Decrement SP
M=2: TICK
     Write low byte
M=3: TICK
```

**The Fix**:
The clock ticks must happen **after** each memory write, not before. The internal delay tick should happen first, then each write should be immediately followed by a tick.

## Summary

The most likely failure reason is **Issue #1: Missing Internal Delay Cycle** combined with **Issue #5: Clock Tick Ordering in PUSH**. The `pushToStack` method in `ControlFlow.java` needs to be restructured so that:

1. M=1: Internal delay tick happens first
2. M=2: SP decrement + high byte write happen together, followed by tick
3. M=3: SP decrement + low byte write happen together, followed by tick

This would match the expected PUSH timing:
- M=0: Instruction decoding
- M=1: Internal delay
- M=2: Memory access for high byte
- M=3: Memory access for low byte

The current implementation has the clock ticks in the wrong positions relative to the memory writes, which causes the timing to be off by one cycle at each step. This would cause the test to fail because the memory writes would occur at different DMA phases than expected.
