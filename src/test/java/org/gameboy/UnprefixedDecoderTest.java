package org.gameboy;

import com.google.gson.GsonBuilder;
import org.gameboy.OpcodeJson.InstructionData;
import org.gameboy.instructions.Instruction;
import org.gameboy.instructions.Unimplemented;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gameboy.instructions.Unimplemented.UNIMPLEMENTED;
import static org.gameboy.utils.BitUtilities.uint;

class UnprefixedDecoderTest {
    private static OpcodeJson opcodeJson;
    private UnprefixedDecoder unprefixedDecoder;

    @BeforeAll
    static void loadJson() throws IOException {
        String jsonString = Files.readString(Path.of(UnprefixedDecoderTest.class.getClassLoader().getResource("Opcodes.json").getFile()));
        opcodeJson = new GsonBuilder().create().fromJson(jsonString, OpcodeJson.class);
    }

    @BeforeEach
    void setup()
    {
        unprefixedDecoder = new UnprefixedDecoder();
    }

    static Stream<Arguments> getAllOpcodes()
    {
        return IntStream.range(0, 0xFF+1)
                .mapToObj(i -> (byte)i)
                .map(b -> Arguments.of(b, getInstruction(getHexRepresentation(b))));
    }

    static String getHexRepresentation(byte opcode)
    {
        return "0x%02X".formatted(uint(opcode));
    }

    static String getInstruction(String hexCode)
    {
        Map<String, InstructionData> instructionTable = opcodeJson.unprefixed();

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
    @MethodSource("getAllOpcodes")
    public void givenNonPrefixedOpcode_whenDecode_thenCorrectInstructionGivenOrUnimplemented(byte opcode, String expectedResult) {
        Instruction decodedInstruction = unprefixedDecoder.decode(opcode);

        if (decodedInstruction != UNIMPLEMENTED) {
            assertThat(decodedInstruction.representation()).isEqualTo(expectedResult);
        }
    }

    @ParameterizedTest(name="{1}")
    @MethodSource("getAllOpcodes")
    public void givenNonPrefixedOpcode_whenDecode_thenNotUnimplemented(byte opcode, String expectedResult) {
        Instruction decodedInstruction = unprefixedDecoder.decode(opcode);

        assertThat(decodedInstruction).isNotEqualTo(UNIMPLEMENTED);
    }
}
