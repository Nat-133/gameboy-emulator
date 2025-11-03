package org.gameboy.cpu;

import org.gameboy.BlarggTestRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BlarggCpuTests {
    private static final String TEST_ROM_DIR = "test-roms/blargg-individual-instructions/";
    private static final int SHORT_TEST_TIMEOUT = 10_000_000;

    @BeforeAll
    public static void checkTestRoms() {
        URL testDir = BlarggCpuTests.class.getClassLoader().getResource(TEST_ROM_DIR);
        if (testDir == null) {
            fail("Test ROM directory not found: " + TEST_ROM_DIR + "\n" +
                 "Please ensure test ROMs are in src/test/resources/" + TEST_ROM_DIR);
        }
    }
    
    @ParameterizedTest(name = "{0}")
    @DisplayName("Individual CPU Instruction Tests")
    @ValueSource(strings = {
        "01-special.gb",
        "02-interrupts.gb",
        "03-op_sp_hl.gb",
        "04-op_r_imm.gb",
        "05-op_rp.gb",
        "06-ld_r_r.gb",
        "07-jr_jp_call_ret_rst.gb",
        "08-misc_instrs.gb",
        "09-op_r_r.gb",
        "10-bit_ops.gb",
        "11-op_a_hl.gb"
    })
    public void testIndividualCpuInstr(String romName) throws Exception {
        String resourcePath = TEST_ROM_DIR + romName;
        InputStream romStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (romStream == null) {
            System.out.println("WARNING: Test ROM not found: " + romName);
            fail("Test ROM not found in resources: " + resourcePath);
        }

        byte[] romData = romStream.readAllBytes();
        romStream.close();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("Running Individual Test: " + romName);
        System.out.println("=".repeat(70) + "\n");
        
        BlarggTestRunner runner = new BlarggTestRunner(romData);
        BlarggTestRunner.TestResult result = runner.runTest(SHORT_TEST_TIMEOUT);
        
        result.print();

        assertTrue(result.passed(), 
            romName + " failed. See output above for details.\n");
    }
}