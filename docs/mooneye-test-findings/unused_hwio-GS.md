# Mooneye Test Analysis: unused_hwio-GS

## Test Overview

The `unused_hwio-GS` test verifies that unused bits in hardware I/O registers and completely unmapped I/O addresses return `1` when read. This test is designed to pass on DMG (original Game Boy), MGB, SGB, and SGB2, but is expected to fail on CGB, AGB, and AGS models where the behavior differs.

The test systematically checks:
1. Unused bits within implemented hardware registers (should read as 1)
2. Completely unmapped I/O addresses in the $FF00-$FF7F range (should read as $FF)

## What The Test Does

The test uses a macro-driven approach to execute multiple test cases:

### Test Execution Flow (per test case):

1. **Setup Phase**:
   - Load test data pointer into HL register
   - Store test address in HRAM for later error reporting
   - Reset stack pointer to default value ($E000)

2. **Test Execution** (`run_testcase` function):
   - Read register address from test data (store in C)
   - Read bit mask from test data (store in B)
   - Read write value from test data
   - Write the value to $FF00+C (the I/O register)
   - Read back the value from $FF00+C
   - Apply the bit mask using AND operation
   - Read expected value from test data
   - Apply the same bit mask to expected value
   - Compare masked read value with masked expected value

3. **Result Checking**:
   - If values match: Continue to next test (RET Z)
   - If values don't match: Print detailed error report and halt

### Specific Test Cases:

The test checks these registers with specific bit masks:

**Registers with Unused Bits:**
- `P1` ($FF00) - bits 7,6 unused: writes $FF and $3F, expects bits 7,6 to read as 1 (result $C0)
- `SC` ($FF02) - bit 0 unused: writes $7E and $00, expects bit 0 to read as 1 (result $7E)
- `TAC` ($FF07) - bits 7-3 unused: writes $F8 and $00, expects bits 7-3 to read as 1 (result $F8)
- `IF` ($FF0F) - bits 7-5 unused: writes $E0 and $00, expects bits 7-5 to read as 1 (result $E0)
- `STAT` ($FF41) - bit 7 unused: writes $80 and $00, expects bit 7 to read as 1 (result $80)
- `NR10` ($FF10) - bit 7 unused: writes $00 and $80, expects bit 7 to read as 1 (result $80)
- `NR30` ($FF1A) - bits 6-0 unused: writes $00 and $7F, expects bits 6-0 to read as 1 (result $7F)
- `NR32` ($FF1C) - bits 7,4-0 unused: writes $00 and $9F, expects those bits to read as 1 (result $9F)
- `NR41` ($FF20) - bits 7-6 unused: writes $00 and $C0, expects bits 7-6 to read as 1 (result $C0)
- `NR44` ($FF23) - bits 5-0 unused: writes $00 and $3F, expects bits 5-0 to read as 1 (result $3F)
- `NR52` ($FF26) - bits 6-4 unused: writes $80 and $F0, expects bits 6-4 to read as 1 (result $70)
- `IE` ($FFFF) - bits 7-5 unused: writes $00 and $E0, expects bits 7-5 to read as 0 on write of $00, but 1 on write of $E0

**Completely Unmapped Registers:**
These should always return $FF when read:
- $FF03, $FF08-$FF0E (timer region gaps)
- $FF15 (sound region gap)
- $FF1F (sound region gap)
- $FF27-$FF29 (sound region gaps)
- $FF4C-$FF7F (large unmapped region)

For each unmapped register, the test:
1. Writes $00, reads back, expects $FF
2. Writes $FF, reads back, expects $FF

## What The Test Expects

### For Registers with Unused Bits:
Each register should return specific bits as `1` regardless of what was written:
- When unused bits are written as 0, they should read back as 1
- When unused bits are written as 1, they should read back as 1
- Only the masked bits (unused bits) are checked

### For Unmapped I/O Addresses:
- Reading any unmapped address should return $FF (all bits set to 1)
- Writes to unmapped addresses have no effect on subsequent reads
- This applies to 52+ unmapped addresses in the $FF00-$FF7F range

### Expected Behavior Summary:
| Register | Address | Mask | Test Values | Expected Result |
|----------|---------|------|-------------|-----------------|
| P1 | $FF00 | $C0 | $FF, $3F | $C0, $C0 |
| SC | $FF02 | $7E | $7E, $00 | $7E, $7E |
| TAC | $FF07 | $F8 | $F8, $00 | $F8, $F8 |
| IF | $FF0F | $E0 | $E0, $00 | $E0, $E0 |
| STAT | $FF41 | $80 | $80, $00 | $80, $80 |
| NR10 | $FF10 | $80 | $00, $80 | $80, $80 |
| NR30 | $FF1A | $7F | $00, $7F | $7F, $7F |
| NR32 | $FF1C | $9F | $00, $9F | $9F, $9F |
| NR41 | $FF20 | $C0 | $00, $C0 | $C0, $C0 |
| NR44 | $FF23 | $3F | $00, $3F | $3F, $3F |
| NR52 | $FF26 | $70 | $80, $F0 | $70, $70 |
| IE | $FFFF | $E0 | $00, $E0 | $00, $E0 |

## What The Test Is Testing

This test validates the Game Boy's memory-mapped I/O behavior for unused hardware:

### 1. Unused Bit Behavior in Implemented Registers
On real DMG hardware, unused bits in I/O registers are connected to pull-up resistors, causing them to read as `1` even when software writes `0` to them. This is fundamental hardware behavior that software may depend on.

### 2. Unmapped I/O Address Behavior
When the CPU reads from an address where no hardware is connected, the data bus floats high (pulled up), resulting in the value $FF being read. This is called "open bus" behavior.

### 3. Hardware Accuracy Requirements
The test ensures emulators properly simulate:
- The physical pull-up resistors on unused register bits
- The data bus pull-up behavior for unmapped addresses
- The write-through behavior (writes are accepted but don't affect unused bits)

### 4. Covered Hardware Components
- Joypad register (P1)
- Serial communication (SC)
- Timer control (TAC)
- Interrupt flags (IF)
- LCD status (STAT)
- Sound registers (NR10, NR30, NR32, NR41, NR44, NR52)
- Interrupt enable (IE)
- Various unmapped I/O ranges

## Potential Failure Reasons

### 1. Missing Sound Hardware Implementation
**Impact: HIGH**

The emulator does not implement any sound/audio registers:
- NR10 ($FF10) - Channel 1 sweep
- NR30 ($FF1A) - Channel 3 enable
- NR32 ($FF1C) - Channel 3 output level
- NR41 ($FF20) - Channel 4 length
- NR44 ($FF23) - Channel 4 control
- NR52 ($FF26) - Sound on/off

**Evidence**: No files found matching sound register names in the codebase.

**Current Behavior**: These addresses likely read from `defaultMemory` array in `MappedMemory`, which initializes to 0, not $FF.

**Expected Fix**: Either implement stub sound registers that return appropriate values with unused bits set to 1, or ensure unmapped I/O addresses return $FF by default.

### 2. Joypad (P1) Register Not Implemented
**Impact: MEDIUM**

The P1 register ($FF00) is not mapped in `MappedMemory.java`:
- The constructor does not include any mapping for address $FF00
- Bits 7 and 6 are unused and should always read as 1

**Current Behavior**: Reads from $FF00 will return `defaultMemory[0xFF00]`, which is likely 0.

**Expected Behavior**: Bits 7-6 should always read as 1 (value OR $C0).

### 3. Serial Control (SC) Unused Bit Not Handled
**Impact: LOW-MEDIUM**

File: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/SerialController.java`

**Current Implementation**:
```java
public byte readSerialControl() {
    return serialControl;
}
```

**Issue**: Bit 0 of SC register is unused and should always read as 1.

**Expected Behavior**: Should return `serialControl | 0x01` or apply a mask of $7E to writable bits.

### 4. Unmapped I/O Addresses Return 0 Instead of $FF
**Impact: HIGH**

File: `/Users/nathaniel.manley/vcs/personal/gameboy-emulator/src/main/java/org/gameboy/common/MappedMemory.java`

**Current Implementation**:
```java
private final byte[] defaultMemory = new byte[0x10000];

@Override
public byte read(short address) {
    int addr = uint(address);
    MemoryLocation mappedValue = memoryMap[addr];
    return (mappedValue != null) ? mappedValue.read() : defaultMemory[addr];
}
```

**Issue**: The `defaultMemory` array initializes to 0 by default in Java. When reading from unmapped I/O addresses ($FF03, $FF08-$FF0E, $FF15, $FF1F, $FF27-$FF29, $FF4C-$FF7F), the emulator returns 0 instead of $FF.

**Expected Behavior**: Unmapped I/O reads in the $FF00-$FF7F range should return $FF (all bits high due to pull-up resistors).

### 5. Interrupt Enable (IE) Register Upper Bits
**Impact: SPECIAL CASE**

The IE register test is unique - it expects upper bits to read as 0 when 0 is written, but as 1 when $E0 is written. This suggests the upper bits ARE writable but unused by the interrupt logic.

**Current Implementation**: Uses `IntBackedRegister` which stores and returns the exact value written.

**Issue**: The test seems to expect that IE properly stores all bits, unlike other registers where unused bits always return 1. This might actually be working correctly, but worth verification.

### 6. Correct Registers Already Implemented

These registers appear to correctly handle unused bits:

**InterruptFlagsRegister** (`IF` at $FF0F):
```java
public byte read() {
    return (byte) (value.get() | UPPER_BITS_MASK); // UPPER_BITS_MASK = 0xE0
}
```
This correctly forces bits 7-5 to read as 1.

**TacRegister** (`TAC` at $FF07):
```java
public byte read() {
    return (byte) (value.get() | UPPER_BITS_MASK); // UPPER_BITS_MASK = 0xF8
}
```
This correctly forces bits 7-3 to read as 1.

**StatRegister** (`STAT` at $FF41):
```java
public byte read() {
    return (byte) (value | 0x80); // Bit 7 always returns 1
}
```
This correctly forces bit 7 to read as 1.

## Summary

The test will likely fail due to:

1. **Missing sound register implementations** - 6 sound registers are completely unmapped
2. **Unmapped I/O returning 0 instead of $FF** - affects 50+ addresses
3. **Missing P1 (joypad) register** - 1 register unmapped
4. **SC register bit 0 not forced to 1** - incorrect bit masking

The most critical fix is ensuring unmapped I/O addresses in the $FF00-$FF7F range return $FF instead of 0. This single change would fix the majority of test failures. Additionally, sound registers need stub implementations that properly handle unused bits.
