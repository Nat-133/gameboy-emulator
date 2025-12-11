# intr_2_oam_ok_timing Test Analysis

## Test Overview

The `intr_2_oam_ok_timing` test verifies the precise timing between when a STAT Mode 2 (OAM scan) interrupt fires and when OAM becomes readable again. This is a critical timing test that validates the exact number of CPU cycles it takes from the moment the interrupt is triggered until OAM memory (0xFE00-0xFE9F) transitions from inaccessible to accessible state.

Test Location: `/Users/nathaniel.manley/vcs/personal/mooneye-test-suite/acceptance/ppu/intr_2_oam_ok_timing.s`

Hardware Verification:
- PASS: DMG, MGB, SGB, SGB2, CGB, AGB, AGS
- FAIL: None (verified on all Game Boy hardware)

## What The Test Does

The test performs two measurement iterations with slightly different delays to determine the exact cycle count:

### Setup Phase (Lines 42-46, 57-59)
1. Sets stack pointer to DEFAULT_SP (0xE000)
2. Waits for V-blank
3. Calls `clear_oam` to initialize OAM memory
4. Sets HL register to point to OAM (0xFE00)
5. Enables only STAT interrupts (writes INTR_STAT to IE register at 0xFFFF)

### Test Iteration Macro (Lines 47-55)
Each test iteration performs:

```assembly
test_iter ARGS delay
  call setup_and_wait_mode2    ; Setup and wait for Mode 2 interrupt
  nops delay                    ; Insert specific delay
  ld b, $00                     ; Initialize counter to 0
- inc b                         ; Increment counter in tight loop
  ld a, (hl)                    ; Try to read from OAM (HL = 0xFE00)
  and $FF                       ; Check if OAM returned valid data
  jr nz, -                      ; Loop until OAM is accessible (non-0xFF)
```

The `setup_and_wait_mode2` subroutine (lines 69-80):
1. Waits for scanline LY = 0x42
2. Waits for Mode 0 (H-Blank)
3. Waits for Mode 3 (Drawing)
4. Writes 0x20 to STAT register (enables Mode 2 OAM interrupt, bit 5)
5. Clears interrupt flags (writes 0 to IF register)
6. Executes EI (enable interrupts)
7. Executes HALT (CPU stops until interrupt fires)
8. After HALT, executes NOP
9. If execution continues past NOP, jumps to fail_halt (test failure)

The STAT interrupt handler (lines 85-87):
```assembly
.org INTR_VEC_STAT
  add sp,+2     ; Pop the return address (skip the "jp fail_halt")
  ret           ; Return from interrupt
```

### First Iteration (Line 60-61)
- Calls `test_iter 46` with 46 NOP delay
- Stores the loop counter result in register D

### Second Iteration (Line 62-63)
- Calls `test_iter 45` with 45 NOP delay
- Stores the loop counter result in register E

### Validation (Lines 64-67)
1. Calls `setup_assertions` to save register state
2. Asserts that register D equals 0x01
3. Asserts that register E equals 0x02
4. Exits with pass/fail based on assertions

## What The Test Expects

The test expects:
- **Register D = 0x01**: After a 46 NOP delay, the tight loop executes exactly 1 time before OAM becomes readable
- **Register E = 0x02**: After a 45 NOP delay, the tight loop executes exactly 2 times before OAM becomes readable

### Timing Analysis

The difference of 1 NOP between the two iterations causes exactly 1 difference in loop count:
- With 46 NOPs: Loop runs 1 time (B=1) before OAM is accessible
- With 45 NOPs: Loop runs 2 times (B=2) before OAM is accessible

This indicates that the critical timing window is precisely between 45 and 46 NOPs after the STAT interrupt handler returns.

### Cycle Breakdown

Starting from HALT instruction:
1. **HALT execution**: CPU stops, waits for interrupt
2. **Mode 2 interrupt fires**: STAT interrupt flag set
3. **Interrupt handling** (~5 M-cycles):
   - 2 M-cycles: Internal delay
   - 1 M-cycle: Push PC to stack (cycle 1)
   - 1 M-cycle: Push PC to stack (cycle 2)
   - 1 M-cycle: Jump to interrupt vector 0x0048
4. **Interrupt handler executes**:
   - `add sp,+2`: 4 M-cycles (pops return address)
   - `ret`: 4 M-cycles (returns from interrupt)
5. **Returns to line after HALT**:
   - `nop`: 1 M-cycle
   - Then either 45 or 46 NOPs
6. **Tight loop**:
   - `ld b, $00`: 2 M-cycles
   - Loop iteration: `inc b` (1 M-cycle) + `ld a, (hl)` (2 M-cycles) + `and $FF` (2 M-cycles) + `jr nz` (3 M-cycles taken or 2 not taken)

The test verifies that OAM becomes accessible at a very specific cycle count after the Mode 2 interrupt fires.

## What The Test Is Testing

This test validates the **OAM accessibility timing during PPU Mode 2 (OAM scan) and the transition to Mode 3 (Drawing)**.

Specifically, it tests:

1. **Mode 2 Interrupt Timing**: When the PPU enters Mode 2 (OAM scan), a STAT interrupt is triggered if bit 5 of STAT is set

2. **OAM Memory Blocking**: During Mode 2 and Mode 3, the CPU cannot access OAM (0xFE00-0xFE9F). Reads from OAM during these modes return 0xFF

3. **OAM Accessibility Window**: OAM becomes accessible only during Mode 0 (H-Blank) and Mode 1 (V-Blank)

4. **Precise Timing**: The exact number of CPU cycles from:
   - Mode 2 interrupt fires
   - Through interrupt handling
   - Until OAM transitions from inaccessible (0xFF) to accessible (actual data)

The test ensures that:
- The emulator correctly blocks OAM access during Mode 2 and Mode 3
- The timing of when OAM becomes accessible after a Mode 2 interrupt matches real hardware
- The transition from Mode 2 → Mode 3 → Mode 0 happens at the correct cycle boundaries

## Potential Failure Reasons

### 1. Missing OAM Access Blocking Implementation

**Primary Issue**: The emulator does not appear to have OAM access blocking based on PPU mode.

**Evidence**:
- No code found that checks PPU mode when reading from OAM addresses (0xFE00-0xFE9F)
- `MappedMemory.java` and `MemoryBus.java` don't check PPU state for OAM reads
- `ObjectAttributeMemory.java` directly reads from memory without mode checking

**Expected Behavior**:
- During Mode 2 (OAM_SCANNING) and Mode 3 (DRAWING): CPU reads from OAM should return 0xFF
- During Mode 0 (H_BLANK) and Mode 1 (V_BLANK): CPU reads from OAM should return actual data

**Code Reference**: `ObjectAttributeMemory.java` lines 16-25:
```java
public short read(int address) {
    if (address > SIZE) {
        return (short) 0;
    }

    byte lower = memory.read((short) (address + START_ADDRESS));
    byte upper = memory.read((short) (address + START_ADDRESS + 1));

    return BitUtilities.concat(upper, lower);
}
```

This method doesn't check the current PPU mode before allowing the read.

### 2. Memory Read Path Doesn't Check PPU State

**Issue**: The memory read path in `MemoryBus.java` only checks for DMA blocking, not PPU mode blocking.

**Code Reference**: `MemoryBus.java` lines 61-67:
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

This only checks DMA state, not PPU mode state.

**Required Fix**: The memory bus or a memory wrapper needs to:
1. Check if the address is in OAM range (0xFE00-0xFE9F)
2. Query the current PPU mode from PpuRegisters
3. Return 0xFF if in Mode 2 or Mode 3
4. Allow normal read if in Mode 0 or Mode 1

### 3. PpuRegisters State Not Accessible to Memory System

**Issue**: The PPU mode is tracked in `PpuRegisters` and used by `DisplayInterruptController`, but the memory system (`MemoryBus`, `MappedMemory`) doesn't have access to PPU state.

**Code Reference**: `PictureProcessingUnit.java` tracks the current mode via the `Step` enum and updates STAT register, but this information isn't used to gate memory access.

**Additional Issue**: `StatParser.java` has a method `setPpuMode()` to write PPU mode to STAT register, but there is **no corresponding getter method** to read the current mode from the STAT register. This means even if the memory system had access to the STAT register, it couldn't extract the current PPU mode.

**Required Architecture**:
- Add a `getPpuMode()` method to `StatParser` that reads bits 0-1 from STAT register and returns the corresponding `PpuMode`
- Memory system needs access to PPU mode information
- Either inject `PpuRegisters` into `MemoryBus`
- Or create an `OamAccessController` that checks PPU mode before allowing reads
- The OAM access check needs to be added to the memory read path in `MemoryBus.read()` method

### 4. Mode Transition Timing

**Issue**: Even if OAM blocking is implemented, the exact cycle timing of mode transitions must be precise.

**Critical Timing Points**:
- Mode 2 starts at cycle 0 of a scanline (OAM becomes inaccessible)
- Mode 2 lasts 80 T-cycles (20 M-cycles)
- Mode 3 starts after Mode 2 (OAM remains inaccessible)
- Mode 3 duration varies (43-80 M-cycles depending on sprites/scroll)
- Mode 0 starts after Mode 3 completes (OAM becomes accessible)

**Code Reference**: `PictureProcessingUnit.java` lines 79-82:
```java
private Step oamScan() {
    oamScanController.performOneClockCycle();
    count++;
    return count < 40*2 ? Step.OAM_SCAN : Step.SCANLINE_SETUP;
}
```

The Mode 2 duration is 40*2 = 80 T-cycles, which is correct. However, the exact T-cycle when `sendDrawing()` is called must match hardware timing.

### 5. Interrupt Handling Timing

**Issue**: The test is extremely sensitive to interrupt handling timing.

**Code Reference**: `HardwareInterrupt.java` lines 8-21:
```java
public static void callInterruptHandler(CpuStructure cpuStructure, Interrupt interrupt) {
    cpuStructure.registers().setIME(false);
    cpuStructure.interruptBus().deactivateInterrupt(interrupt);

    cpuStructure.registers().setPC(cpuStructure.idu().decrement(cpuStructure.registers().PC()));

    cpuStructure.clock().tick();

    ControlFlow.pushToStack(cpuStructure, cpuStructure.registers().PC());

    cpuStructure.registers().setPC(interrupt.getInterruptHandlerAddress());
    byte nextInstruction = ControlFlow.readIndirectPCAndIncrement(cpuStructure);
    cpuStructure.registers().setInstructionRegister(nextInstruction);
}
```

The interrupt handling must consume exactly the right number of M-cycles:
- 2 M-cycles for internal operations
- 2 M-cycles for pushing PC high byte
- 2 M-cycles for pushing PC low byte
- 1 M-cycle for jumping to handler

Any deviation will cause the test to fail.

### 6. HALT Instruction Wake-Up Timing

**Issue**: The timing from HALT to interrupt handler execution must be exact.

**Code Reference**: `Halt.java` lines 15-25:
```java
@Override
public void execute(CpuStructure cpuStructure) {
    boolean IME = cpuStructure.registers().IME();
    boolean interruptsPending = cpuStructure.interruptBus().hasInterrupts();

    if (!IME && interruptsPending) {
        cpuStructure.idu().disableNextIncrement();
    }

    cpuStructure.clock().stop();
    cpuStructure.interruptBus().waitForInterrupt();
    cpuStructure.clock().start();
}
```

The `waitForInterrupt()` method (in `InterruptBus.java` lines 55-59) ticks the clock until an interrupt is detected:
```java
public void waitForInterrupt() {
    while (!hasInterrupts()) {
        clock.tick();
    }
}
```

This ensures HALT correctly advances cycles until the interrupt fires.

## Summary

The most likely reason this test would fail is **missing OAM access blocking based on PPU mode**. The emulator needs to:

1. Implement OAM read blocking that returns 0xFF during Mode 2 and Mode 3
2. Allow normal OAM reads during Mode 0 and Mode 1
3. Ensure the memory read path checks PPU mode for addresses in range 0xFE00-0xFE9F
4. Verify precise cycle-accurate timing of mode transitions

The test is checking a very specific timing window (difference of 1 NOP = 4 T-cycles) to validate that OAM becomes accessible at exactly the right moment relative to the Mode 2 interrupt, which requires both correct OAM blocking and precise PPU timing.

### Implementation Roadmap

To fix this test, implement in this order:

1. **Add `getPpuMode()` to StatParser.java**:
   ```java
   static PpuMode getPpuMode(byte stat) {
       int mode = stat & 0b0000_0011;
       return switch(mode) {
           case 0 -> PpuMode.H_BLANK;
           case 1 -> PpuMode.V_BLANK;
           case 2 -> PpuMode.OAM_SCANNING;
           case 3 -> PpuMode.DRAWING;
           default -> throw new IllegalStateException("Invalid PPU mode: " + mode);
       };
   }
   ```

2. **Add public mode getter to PpuRegisters.java**:
   ```java
   public StatParser.PpuMode getCurrentMode() {
       byte statValue = read(PpuRegister.STAT);
       return StatParser.getPpuMode(statValue);
   }
   ```

3. **Modify MemoryBus.read() to check OAM accessibility**:
   - Inject `PpuRegisters` into `MemoryBus` constructor
   - In the `read()` method, before line 63, add a check:
     ```java
     if (isOamAddress(address) && !isOamAccessible()) {
         return (byte) 0xFF;
     }
     ```
   - Add helper methods:
     ```java
     private boolean isOamAddress(short address) {
         int addr = address & 0xFFFF;
         return addr >= 0xFE00 && addr <= 0xFE9F;
     }

     private boolean isOamAccessible() {
         StatParser.PpuMode mode = ppuRegisters.getCurrentMode();
         return mode == StatParser.PpuMode.H_BLANK ||
                mode == StatParser.PpuMode.V_BLANK;
     }
     ```

4. **Verify timing**: Once implemented, run the test. If it still fails with different values in D and E, the PPU timing may need adjustment in `PictureProcessingUnit.java`.
