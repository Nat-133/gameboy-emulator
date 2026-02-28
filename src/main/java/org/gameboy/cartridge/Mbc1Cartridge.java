package org.gameboy.cartridge;

import org.gameboy.common.Cartridge;

import static org.gameboy.utils.BitUtilities.uint;

public class Mbc1Cartridge implements Cartridge {
    private final byte[] rom;
    private final byte[] ram;
    private final int romBankMask;
    private final int ramBankMask;

    private int bank1 = 1;
    private int bank2 = 0;
    private int bankingMode = 0;
    private boolean ramEnabled = false;

    public Mbc1Cartridge(byte[] romData) {
        CartridgeHeader header = new CartridgeHeader(romData);
        this.rom = romData;
        this.romBankMask = header.romBankMask();
        this.ramBankMask = header.ramBankMask();

        int ramSize = header.ramBankCount() * 0x2000;
        this.ram = new byte[ramSize];
    }

    @Override
    public byte read(short address) {
        int addr = uint(address);

        if (addr <= 0x3FFF) {
            if (bankingMode == 1) {
                int effectiveBank = (bank2 << 5) & romBankMask;
                int romOffset = effectiveBank * 0x4000 + addr;
                if (romOffset < rom.length) {
                    return rom[romOffset];
                }
                return (byte) 0xFF;
            }
            return rom[addr];
        }

        if (addr <= 0x7FFF) {
            int bank = (bank2 << 5) | bank1;
            int effectiveBank = bank & romBankMask;
            int romOffset = effectiveBank * 0x4000 + (addr - 0x4000);
            if (romOffset < rom.length) {
                return rom[romOffset];
            }
            return (byte) 0xFF;
        }

        if (addr >= 0xA000 && addr <= 0xBFFF) {
            if (!ramEnabled || ram.length == 0) {
                return (byte) 0xFF;
            }

            int effectiveRamBank;
            if (bankingMode == 1) {
                effectiveRamBank = ramBankMask > 0 ? bank2 & ramBankMask : 0;
            } else {
                effectiveRamBank = 0;
            }

            int ramOffset = effectiveRamBank * 0x2000 + (addr - 0xA000);
            if (ramOffset < ram.length) {
                return ram[ramOffset];
            }
            return (byte) 0xFF;
        }

        return (byte) 0xFF;
    }

    @Override
    public void write(short address, byte value) {
        int addr = uint(address);
        int val = Byte.toUnsignedInt(value);

        if (addr <= 0x1FFF) {
            ramEnabled = ((val & 0x0F) == 0x0A);
            return;
        }

        if (addr <= 0x3FFF) {
            int bank = val & 0x1F;
            bank1 = (bank == 0) ? 1 : bank;
            return;
        }

        if (addr <= 0x5FFF) {
            bank2 = val & 0x03;
            return;
        }

        if (addr <= 0x7FFF) {
            bankingMode = val & 0x01;
            return;
        }

        if (addr >= 0xA000 && addr <= 0xBFFF) {
            if (!ramEnabled || ram.length == 0) {
                return;
            }

            int effectiveRamBank;
            if (bankingMode == 1) {
                effectiveRamBank = ramBankMask > 0 ? bank2 & ramBankMask : 0;
            } else {
                effectiveRamBank = 0;
            }

            int ramOffset = effectiveRamBank * 0x2000 + (addr - 0xA000);
            if (ramOffset < ram.length) {
                ram[ramOffset] = value;
            }
        }
    }
}
