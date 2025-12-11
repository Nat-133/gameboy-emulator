# Mooneye Test Analysis: sources-GS

## Test Overview

The `sources-GS` test validates that OAM DMA (Direct Memory Access) correctly reads from all possible source memory regions, including the special behavior for addresses beyond $DFFF. This is a DMG (original Game Boy) specific test that verifies DMA can read from ROM, VRAM, external RAM, work RAM, echo RAM, OAM, and high RAM regions.

**Note:** This test is expected to PASS on DMG/MGB/SGB/SGB2 but FAIL on CGB/AGB/AGS according to the test comments.

## What The Test Does

The test performs a comprehensive check of OAM DMA source address handling across the entire 16-bit address space. Here's the step-by-step execution:

### Initial Setup (lines 33-38)
1. Sets stack pointer to $FFFF
2. Disables PPU for safe memory operations
3. Clears OAM, VRAM, and WRAM regions
4. Copies DMA transfer procedure to HRAM for execution during DMA

### Test Part 1: ROM Regions (lines 46-88)

**Test $0000 (lines 46-55):**
- Initiates DMA from source address $0000
- Expects OAM to contain the 160 bytes from $0000-$009F (ROM bank 0)
- ROM contains: `$1d, $de, $f9, $11, $5f, ...` (defined at line 273)

**Test $3F00 (lines 57-66):**
- Initiates DMA from source address $3F00
- Expects OAM to contain the 160 bytes from $3F00-$3F9F (ROM bank 0)
- ROM contains: `$c1, $cc, $59, $fd, $44, ...` (defined at line 285)

**Test $4000 (lines 68-77):**
- Initiates DMA from source address $4000
- Expects OAM to contain the 160 bytes from $4000-$409F (ROM bank 1)
- ROM contains: `$56, $44, $c8, $f8, $be, ...` (defined at line 299)

**Test $7F00 (lines 79-88):**
- Initiates DMA from source address $7F00
- Expects OAM to contain the 160 bytes from $7F00-$7F9F (ROM bank 1)
- ROM contains: `$51, $7b, $1a, $54, $7c, ...` (defined at line 311)

### Test Part 2: VRAM Regions (lines 90-116)

**Preparation (lines 91-94):**
- Copies `ram_pattern_1` to $8000 in VRAM
- Copies `ram_pattern_2` to $9F00 in VRAM

**Test $8000 (lines 96-105):**
- Initiates DMA from source address $8000
- Expects OAM to contain `ram_pattern_1`: `$c2, $d5, $1a, $e9, $fb, ...` (line 244)

**Test $9F00 (lines 107-116):**
- Initiates DMA from source address $9F00
- Expects OAM to contain `ram_pattern_2`: `$db, $16, $e1, $18, $00, ...` (line 261)

### Test Part 3: External RAM Regions (lines 118-154)

**Preparation (lines 121-132):**
- Enables external RAM by writing $0A to $0000
- Clears $A000-$BFFF (8KB external RAM)
- Copies `ram_pattern_1` to $A000
- Copies `ram_pattern_2` to $BF00

**Test $A000 (lines 134-143):**
- Initiates DMA from source address $A000
- Expects OAM to contain `ram_pattern_1`

**Test $BF00 (lines 145-154):**
- Initiates DMA from source address $BF00
- Expects OAM to contain `ram_pattern_2`

### Test Part 4: Work RAM, Echo RAM, and High RAM (lines 156-222)

**Preparation (lines 157-165):**
- Disables external RAM by writing $00 to $0000
- Copies `ram_pattern_1` to $C000 (work RAM)
- Copies `ram_pattern_1` to $DE00 (echo RAM mirrors $C000-$DDFF)
- Copies `ram_pattern_2` to $DF00

**Test $C000 (lines 167-176):**
- Initiates DMA from source address $C000
- Expects OAM to contain `ram_pattern_1`

**Test $DF00 (lines 178-187):**
- Initiates DMA from source address $DF00
- Expects OAM to contain `ram_pattern_2`

**Test $E000 (lines 189-198):**
- Initiates DMA from source address $E000 (echo RAM region)
- **Critical:** Expects OAM to contain `ram_pattern_1` (mirroring $C000)
- This tests that DMA properly mirrors echo RAM ($E000-$FDFF mirrors $C000-$DDFF)

**Test $FE00 (lines 200-210):**
- Clears OAM first
- Initiates DMA from source address $FE00 (OAM itself!)
- Expects OAM to contain `ram_pattern_1`
- **Critical:** Tests DMA reading from OAM memory

**Test $FF00 (lines 212-221):**
- Initiates DMA from source address $FF00 (I/O registers region)
- Expects OAM to contain `ram_pattern_2`
- **Critical:** Tests DMA reading from high memory/I/O region

### DMA Procedure (lines 231-236)

The actual DMA transfer code that runs from HRAM:
```assembly
dma_proc:
  ldh (<DMA), a      ; Write source high byte to DMA register ($FF46)
  ld a, 40           ; Load 40 into accumulator
- dec a              ; Decrement (loop 40 times)
  jr nz, -           ; Jump if not zero
  ret                ; Return (DMA completes in ~160 cycles)
```

This procedure:
1. Writes to the DMA register to initiate transfer
2. Waits approximately 160 M-cycles for DMA to complete
3. Returns control to test code

## What The Test Expects

For each DMA transfer test, the test expects:

1. **DMA Register Write:** Writing to $FF46 should trigger DMA transfer
2. **Source Address Calculation:** Source address = (byte written to $FF46) << 8
3. **Transfer Size:** Exactly 160 bytes transferred from source to OAM ($FE00-$FE9F)
4. **Memory Region Behavior:**
   - **$0000-$7FFF:** Read from ROM (banks 0 and 1)
   - **$8000-$9FFF:** Read from VRAM
   - **$A000-$BFFF:** Read from external RAM (when enabled)
   - **$C000-$DFFF:** Read from work RAM
   - **$E000-$FDFF:** Mirror $C000-$DDFF (echo RAM)
   - **$FE00-$FE9F:** Read from OAM itself
   - **$FF00-$FF7F:** Read from I/O registers region
   - **$FF80-$FFFE:** Read from high RAM (HRAM)

5. **Byte-by-Byte Comparison:** Each byte in OAM must exactly match the expected pattern

## What The Test Is Testing

This test validates several critical aspects of the Game Boy's OAM DMA system:

### 1. DMA Source Address Decoding
- DMA must correctly interpret the 8-bit value written to $FF46 as the high byte of a 16-bit address
- Must support all memory regions, not just specific ranges

### 2. Memory Region Access During DMA
- DMA reads must access the correct underlying memory for each address range
- ROM banking must work correctly (bank 0 vs bank 1 for $0000-$3FFF vs $4000-$7FFF)
- External RAM must be accessible when enabled via MBC control

### 3. Echo RAM Mirroring ($E000-$FDFF)
- **Critical behavior:** Addresses $E000-$FDFF must mirror $C000-$DDFF
- Reading from $E000 during DMA should return the same data as $C000
- This is a hardware quirk specific to DMG/MGB/SGB

### 4. Special Region Handling
- **OAM reads:** DMA can read from OAM memory itself ($FE00-$FE9F)
- **I/O region reads:** DMA can read from I/O registers ($FF00-$FF7F)
- **HRAM reads:** DMA can read from high RAM ($FF80-$FFFE)

### 5. DMA Timing and Completion
- DMA must complete the full 160-byte transfer
- The delay loop (40 iterations) provides sufficient time for DMA to finish

## Potential Failure Reasons

Based on analysis of the emulator code, here are the likely reasons this test might fail:

### 1. Missing Memory Bank Controller (MBC) Support
**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryInitializer.java`

**Issue:**
```java
byte[] gameRom = romLoader.loadRom(gameRomPath, 0x8000); // 32KB max for basic ROMs
dumps.add(MemoryDump.fromZero(gameRom));
```

The emulator loads the ROM as a flat 32KB dump starting at address $0000. There is no evidence of:
- MBC5 support (test uses `.define CART_TYPE $1B` which is MBC5)
- ROM banking implementation
- External RAM banking
- Cartridge RAM enable/disable logic

**Impact:** Tests reading from $4000-$7FFF (ROM bank 1) and $A000-$BFFF (external RAM) will fail because:
- $4000-$7FFF will read from the wrong ROM data (bank 0 instead of bank 1)
- $A000-$BFFF reads will fail or return incorrect data (no external RAM implementation)

### 2. Missing Echo RAM Mirroring ($E000-$FDFF)
**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MappedMemory.java`

**Issue:**
```java
@Override
public byte read(short address) {
    int addr = uint(address);
    MemoryLocation mappedValue = memoryMap[addr];
    return (mappedValue != null) ? mappedValue.read() : defaultMemory[addr];
}
```

The memory system treats $E000-$FDFF as distinct from $C000-$DDFF. There is no logic to:
- Mirror reads from $E000-$FDFF to $C000-$DDFF
- Redirect address $E000 to $C000, etc.

**Impact:** The test at $E000 expects to read data from $C000 but will instead read from $E000 (which is different or uninitialized).

**Game Boy Hardware Behavior:**
On real hardware, addresses $E000-$FDFF are "echo RAM" - they physically map to the same memory as $C000-$DDFF. Reading $E000 returns the same value as $C000.

### 3. DMA Reading from Special Regions
**Location:** `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MemoryBus.java`

**Current Implementation:**
```java
case TRANSFERRING -> {
    short sourceAddr = (short) (dmaSourceAddress + dmaByteIndex);
    short destAddr = (short) (OAM_START_ADDRESS + dmaByteIndex);
    byte data = underlying.read(sourceAddr);  // Uses normal read path
    underlying.write(destAddr, data);
    dmaByteIndex++;
    // ...
}
```

The DMA transfer uses `underlying.read()` which should work for most regions. However:

**Potential Issues:**
1. **OAM reads ($FE00):** DMA reading from OAM itself might have special behavior
2. **I/O register reads ($FF00-$FF7F):** Reading I/O registers during DMA might not work correctly
3. **HRAM reads ($FF80-$FFFE):** These should work but might have edge cases

### 4. Unimplemented Memory Regions
**Location:** Multiple files

**Missing Implementation:**
- No cartridge ROM banking logic
- No external RAM controller
- No MBC5 register handlers at $0000 (RAM enable), $2000-$2FFF (ROM bank low), $3000 (ROM bank high), $4000-$5FFF (RAM bank)

**Impact:**
- Writing $0A to $0000 (line 122) to enable external RAM has no effect
- Writing $00 to $0000 (line 158) to disable external RAM has no effect
- External RAM at $A000-$BFFF is not properly implemented

### 5. Address Range Handling Above $DFFF
**Location:** DMA implementation doesn't have special handling

**Observation:**
The test specifically mentions this is checking "the area past $DFFF." The tests at $E000, $FE00, and $FF00 are critical.

**Potential Issue:**
If the emulator doesn't properly handle:
- Echo RAM mirroring ($E000-$FDFF → $C000-$DDFF)
- Prohibited region ($FEA0-$FEFF)
- I/O region during DMA ($FF00-$FF7F)

These tests will fail with incorrect data.

## Summary of Required Fixes

To pass this test, the emulator needs:

1. **MBC5 Implementation:**
   - ROM banking registers at $2000-$2FFF (low 8 bits) and $3000 (9th bit)
   - External RAM enable/disable register at $0000-$1FFF
   - RAM banking register at $4000-$5FFF
   - Proper ROM bank switching for $4000-$7FFF region
   - External RAM at $A000-$BFFF with bank switching

2. **Echo RAM Mirroring:**
   - Reads from $E000-$FDFF must return data from $C000-$DDFF
   - Formula: `if (addr >= 0xE000 && addr <= 0xFDFF) { addr = addr - 0x2000; }`

3. **DMA Special Region Support:**
   - Ensure DMA can read from all memory regions including OAM and I/O
   - Verify HRAM reads work correctly during DMA

4. **Memory Architecture Refactoring:**
   - Implement proper memory banking infrastructure
   - Add MBC5 controller class
   - Support dynamic ROM/RAM bank switching

The test is comprehensive and tests the full address space, making it an excellent test for validating complete memory system implementation.
