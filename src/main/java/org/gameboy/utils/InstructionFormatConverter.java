package org.gameboy.utils;

/**
 * Converts instruction string representations between the emulator's internal format
 * and the reference log format used for comparison/debugging.
 *
 * Emulator format examples:
 *   JP imm16, XOR A,A, LD HL,imm16, JR NZ,e8, LDH (imm8),A
 *
 * Reference format examples:
 *   JP a16, XOR A, LD HL,d16, JR NZ,r8, LD (a8),A
 */
public final class InstructionFormatConverter {

    private InstructionFormatConverter() {
        // Static utility class
    }

    /**
     * Converts from emulator's internal format to reference log format.
     */
    public static String toReferenceFormat(String emulatorFormat) {
        String result = emulatorFormat;

        // Convert LDH to LD with a8 operand
        // LDH (imm8),A -> LD (a8),A
        // LDH A,(imm8) -> LD A,(a8)
        if (result.startsWith("LDH ")) {
            result = "LD " + result.substring(4);
        }

        // Convert immediate operands
        // imm16 -> a16 for control flow instructions (JP, CALL)
        // imm16 -> d16 for data instructions (LD)
        if (result.startsWith("JP ") || result.startsWith("CALL ")) {
            result = result.replace("imm16", "a16");
        } else {
            result = result.replace("imm16", "d16");
        }

        // imm8 -> d8 for general use, but a8 for high memory access
        // (imm8) -> (a8) for high memory
        result = result.replace("(imm8)", "(a8)");
        result = result.replace("imm8", "d8");

        // e8 -> r8 for relative jumps
        result = result.replace(",e8", ",r8");
        result = result.replace(" e8", " r8");

        // SP+e8 -> SP+r8
        result = result.replace("SP+e8", "SP+r8");

        // Remove implicit A operand for ALU operations
        // XOR A,X -> XOR X
        // AND A,X -> AND X
        // OR A,X -> OR X
        // CP A,X -> CP X (but only when there's a second operand)
        result = convertAluInstruction(result, "XOR");
        result = convertAluInstruction(result, "AND");
        result = convertAluInstruction(result, "OR");
        result = convertAluInstruction(result, "CP");
        result = convertAluInstruction(result, "SUB");
        result = convertAluInstruction(result, "SBC");
        result = convertAluInstruction(result, "ADC");

        return result;
    }

    /**
     * Converts from reference log format to emulator's internal format.
     */
    public static String toEmulatorFormat(String referenceFormat) {
        String result = referenceFormat;

        // Convert LD (a8) back to LDH (imm8)
        if (result.contains("(a8)")) {
            result = result.replace("LD ", "LDH ");
            result = result.replace("(a8)", "(imm8)");
        }

        // Convert immediate operands back
        result = result.replace("a16", "imm16");
        result = result.replace("d16", "imm16");
        result = result.replace("d8", "imm8");
        result = result.replace(",r8", ",e8");
        result = result.replace(" r8", " e8");
        result = result.replace("SP+r8", "SP+e8");

        // Add back implicit A operand for ALU operations
        result = unconvertAluInstruction(result, "XOR");
        result = unconvertAluInstruction(result, "AND");
        result = unconvertAluInstruction(result, "OR");
        result = unconvertAluInstruction(result, "CP");
        result = unconvertAluInstruction(result, "SUB");
        result = unconvertAluInstruction(result, "SBC");
        result = unconvertAluInstruction(result, "ADC");

        return result;
    }

    private static String convertAluInstruction(String input, String mnemonic) {
        String prefix = mnemonic + " A,";
        if (input.startsWith(prefix)) {
            // XOR A,A -> XOR A, XOR A,B -> XOR B, etc.
            return mnemonic + " " + input.substring(prefix.length());
        }
        return input;
    }

    private static String unconvertAluInstruction(String input, String mnemonic) {
        String prefix = mnemonic + " ";
        if (input.startsWith(prefix) && !input.startsWith(mnemonic + " A,")) {
            String operand = input.substring(prefix.length());
            // Don't convert if it's already in the form "MNEMONIC A,X"
            if (!operand.startsWith("A,")) {
                return mnemonic + " A," + operand;
            }
        }
        return input;
    }
}
