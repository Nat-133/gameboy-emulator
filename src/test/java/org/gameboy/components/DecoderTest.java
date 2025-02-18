package org.gameboy.components;

import org.gameboy.instructions.Add;
import org.gameboy.instructions.Nop;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DecoderTest {
    @Test
    void givenDecoder_whenDecode_thenDefaultToUnprefixedTable() {
        OpcodeTable unprefixed = opcode -> Nop.nop();
        OpcodeTable prefixed = opcode -> Add.add_a_imm8();
        Decoder decoder = new Decoder(unprefixed, prefixed);

        assertThat(decoder.decode((byte) 0xab)).isEqualTo(Nop.nop());
    }

    @Test
    void givenDecoder_whenSwitchTables_thenPrefixedTableUsed() {
        OpcodeTable unprefixed = opcode -> Nop.nop();
        OpcodeTable prefixed = opcode -> Add.add_a_imm8();
        Decoder decoder = new Decoder(unprefixed, prefixed);

        decoder.switchTables();

        assertThat(decoder.decode((byte) 0xab)).isEqualTo(Add.add_a_imm8());
    }

    @Test
    void givenDecoder_whenSwitchTables_thenUnprefixedTableUsedAfterOneDecode() {
        OpcodeTable unprefixed = opcode -> Nop.nop();
        OpcodeTable prefixed = opcode -> Add.add_a_imm8();
        Decoder decoder = new Decoder(unprefixed, prefixed);

        decoder.switchTables();
        decoder.decode((byte) 0xab);

        assertThat(decoder.decode((byte) 0xab)).isEqualTo(Nop.nop());
    }
}