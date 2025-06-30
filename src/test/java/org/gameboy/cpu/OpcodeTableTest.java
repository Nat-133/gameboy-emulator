package org.gameboy.cpu;

import com.google.gson.GsonBuilder;
import org.gameboy.OpcodeJson;
import org.gameboy.OpcodeJson.InstructionData;
import org.gameboy.cpu.components.PrefixedOpcodeTable;
import org.gameboy.cpu.components.UnprefixedOpcodeTable;
import org.gameboy.cpu.instructions.Instruction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.cpu.instructions.Unimplemented.UNIMPLEMENTED;
import static org.gameboy.utils.BitUtilities.uint;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class OpcodeTableTest {
    private static OpcodeJson opcodeJson;

    @BeforeAll
    static void loadJson() throws IOException {
        String jsonString;
        try (InputStream resource = OpcodeTableTest.class.getClassLoader().getResourceAsStream("Opcodes.json.gz")) {
            assert resource != null;

            GZIPInputStream gzipInputStream = new GZIPInputStream(resource);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            jsonString = byteArrayOutputStream.toString();
        }

        opcodeJson = new GsonBuilder().create().fromJson(jsonString, OpcodeJson.class);
    }

    static Stream<Arguments> getAllUnprefixedOpcodes()
    {
        return IntStream.range(0, 0xFF+1)
                .mapToObj(i -> (byte)i)
                .map(b -> Arguments.of(b, getInstruction(getHexRepresentation(b), opcodeJson.unprefixed())));
    }

    static Stream<Arguments> getAllPrefixedOpcodes()
    {
        return IntStream.range(0, 0xFF+1)
                .mapToObj(i -> (byte)i)
                .map(b -> Arguments.of(b, getInstruction(getHexRepresentation(b), opcodeJson.prefixed())));
    }

    static String getHexRepresentation(byte opcode)
    {
        return "0x%02X".formatted(uint(opcode));
    }

    static String getInstruction(String hexCode, Map<String, InstructionData> instructionTable)
    {
        InstructionData instructionData = instructionTable.get(hexCode);

        String targets = instructionData.operands().length > 0 ? " " : "";
        targets += Arrays.stream(instructionData.operands())
                .map(operandData -> {
                    String name = operandData.name();
                    name += operandData.increment() ? "+" : "";
                    name += operandData.decrement() ? "-" : "";
                    name = operandData.immediate() ? name : "(" + name + ")";
                    return name;
                })
                .collect(Collectors.joining(","));

        return instructionData.mnemonic() + targets;
    }

    @ParameterizedTest(name="{1}")
    @MethodSource("getAllUnprefixedOpcodes")
    public void givenNonPrefixedOpcode_whenLookup_thenCorrectInstructionGivenOrUnimplemented(byte opcode, String expectedResult) {
        UnprefixedOpcodeTable unprefixedOpcodeTable = new UnprefixedOpcodeTable();
        Instruction decodedInstruction = unprefixedOpcodeTable.lookup(opcode);

        assumeTrue(decodedInstruction != UNIMPLEMENTED);
        assertThat(decodedInstruction.representation()).isEqualTo(expectedResult);
    }


    @ParameterizedTest(name="{1}")
    @MethodSource("getAllPrefixedOpcodes")
    public void givenPrefixedOpcode_whenLookup_thenCorrectInstructionGivenOrUnimplemented(byte opcode, String expectedResult) {
        PrefixedOpcodeTable prefixedOpcodeTable = new PrefixedOpcodeTable();
        Instruction decodedInstruction = prefixedOpcodeTable.lookup(opcode);

        assumeTrue(decodedInstruction != UNIMPLEMENTED);
        assertThat(decodedInstruction.representation()).isEqualTo(expectedResult);
    }
}
