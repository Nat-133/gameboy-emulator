package org.gameboy.cpu.instructions.targets;

import org.gameboy.utils.MultiBitValue.ThreeBitValue;
import org.gameboy.utils.MultiBitValue.TwoBitValue;

public sealed interface Target
        permits Target.A, Target.B, Target.C, Target.D, Target.E, Target.H, Target.L,
                Target.BC, Target.DE, Target.HL, Target.SP, Target.AF, Target.PC,
                Target.IndirectHL, Target.IndirectBC, Target.IndirectDE,
                Target.IndirectHLInc, Target.IndirectHLDec,
                Target.IndirectC, Target.IndirectImm8, Target.IndirectImm16,
                Target.Imm8, Target.Imm16, Target.SPOffset {
    String representation();

    // ── Grouping interfaces ───────────────────────────────────────────

    sealed interface ByteTarget
            permits A, B, C, D, E, H, L,
                    IndirectHL, IndirectBC, IndirectDE, IndirectHLInc, IndirectHLDec,
                    IndirectC, IndirectImm8, IndirectImm16, Imm8 {
        String representation();
    }

    sealed interface WordTarget
            permits BC, DE, HL, SP, AF, PC, Imm16, SPOffset {
        String representation();
    }

    sealed interface RegisterTarget
            permits A, B, C, D, E, H, L, BC, DE, HL, SP, AF, PC {
        String representation();
    }

    sealed interface IndirectTarget
            permits IndirectHL, IndirectBC, IndirectDE, IndirectHLInc, IndirectHLDec,
                    IndirectC, IndirectImm8, IndirectImm16 {
        String representation();
    }

    // ── Opcode grouping interfaces ────────────────────────────────────

    sealed interface R8
            permits A, B, C, D, E, H, L, IndirectHL {
        String representation();
        R8[] LOOKUP_TABLE = {
                B.INSTANCE, C.INSTANCE, D.INSTANCE, E.INSTANCE,
                H.INSTANCE, L.INSTANCE, IndirectHL.INSTANCE, A.INSTANCE
        };

        static R8 lookup(ThreeBitValue value) {
            return LOOKUP_TABLE[value.value()];
        }
    }

    sealed interface R16
            permits BC, DE, HL, SP {
        String representation();
        R16[] LOOKUP_TABLE = {
                BC.INSTANCE, DE.INSTANCE, HL.INSTANCE, SP.INSTANCE
        };

        static R16 lookup(TwoBitValue value) {
            return LOOKUP_TABLE[value.value()];
        }
    }

    sealed interface Stk16
            permits BC, DE, HL, AF {
        String representation();
        Stk16[] LOOKUP_TABLE = {
                BC.INSTANCE, DE.INSTANCE, HL.INSTANCE, AF.INSTANCE
        };

        static Stk16 lookup(TwoBitValue value) {
            return LOOKUP_TABLE[value.value()];
        }
    }

    sealed interface Mem16
            permits IndirectBC, IndirectDE, IndirectHLInc, IndirectHLDec {
        String representation();
        Mem16[] LOOKUP_TABLE = {
                IndirectBC.INSTANCE, IndirectDE.INSTANCE,
                IndirectHLInc.INSTANCE, IndirectHLDec.INSTANCE
        };

        static Mem16 lookup(TwoBitValue value) {
            return LOOKUP_TABLE[value.value()];
        }
    }

    // ── Concrete singleton enums ──────────────────────────────────────

    enum A implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "A"; }
        @Override public String toString() { return representation(); }
    }

    enum B implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "B"; }
        @Override public String toString() { return representation(); }
    }

    enum C implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "C"; }
        @Override public String toString() { return representation(); }
    }

    enum D implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "D"; }
        @Override public String toString() { return representation(); }
    }

    enum E implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "E"; }
        @Override public String toString() { return representation(); }
    }

    enum H implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "H"; }
        @Override public String toString() { return representation(); }
    }

    enum L implements Target, R8, ByteTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "L"; }
        @Override public String toString() { return representation(); }
    }

    enum BC implements Target, R16, Stk16, WordTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "BC"; }
        @Override public String toString() { return representation(); }
    }

    enum DE implements Target, R16, Stk16, WordTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "DE"; }
        @Override public String toString() { return representation(); }
    }

    enum HL implements Target, R16, Stk16, WordTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "HL"; }
        @Override public String toString() { return representation(); }
    }

    enum SP implements Target, R16, WordTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "SP"; }
        @Override public String toString() { return representation(); }
    }

    enum AF implements Target, Stk16, WordTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "AF"; }
        @Override public String toString() { return representation(); }
    }

    enum PC implements Target, WordTarget, RegisterTarget {
        INSTANCE;
        @Override public String representation() { return "PC"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectHL implements Target, R8, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(HL)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectBC implements Target, Mem16, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(BC)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectDE implements Target, Mem16, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(DE)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectHLInc implements Target, Mem16, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(HL+)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectHLDec implements Target, Mem16, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(HL-)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectC implements Target, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(C)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectImm8 implements Target, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(imm8)"; }
        @Override public String toString() { return representation(); }
    }

    enum IndirectImm16 implements Target, ByteTarget, IndirectTarget {
        INSTANCE;
        @Override public String representation() { return "(imm16)"; }
        @Override public String toString() { return representation(); }
    }

    enum Imm8 implements Target, ByteTarget {
        INSTANCE;
        @Override public String representation() { return "imm8"; }
        @Override public String toString() { return representation(); }
    }

    enum Imm16 implements Target, WordTarget {
        INSTANCE;
        @Override public String representation() { return "imm16"; }
        @Override public String toString() { return representation(); }
    }

    enum SPOffset implements Target, WordTarget {
        INSTANCE;
        @Override public String representation() { return "SP+e8"; }
        @Override public String toString() { return representation(); }
    }

    // ── Convenience constants ─────────────────────────────────────────

    A a = A.INSTANCE;
    B b = B.INSTANCE;
    C c = C.INSTANCE;
    D d = D.INSTANCE;
    E e = E.INSTANCE;
    H h = H.INSTANCE;
    L l = L.INSTANCE;
    BC bc = BC.INSTANCE;
    DE de = DE.INSTANCE;
    HL hl = HL.INSTANCE;
    SP sp = SP.INSTANCE;
    AF af = AF.INSTANCE;
    PC pc = PC.INSTANCE;
    IndirectHL indirect_hl = IndirectHL.INSTANCE;
    IndirectBC indirect_bc = IndirectBC.INSTANCE;
    IndirectDE indirect_de = IndirectDE.INSTANCE;
    IndirectHLInc indirect_hl_inc = IndirectHLInc.INSTANCE;
    IndirectHLDec indirect_hl_dec = IndirectHLDec.INSTANCE;
    IndirectC indirect_c = IndirectC.INSTANCE;
    IndirectImm8 indirect_imm_8 = IndirectImm8.INSTANCE;
    IndirectImm16 indirect_imm_16 = IndirectImm16.INSTANCE;
    Imm8 imm_8 = Imm8.INSTANCE;
    Imm16 imm_16 = Imm16.INSTANCE;
    SPOffset sp_offset = SPOffset.INSTANCE;
}
