package org.gameboy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class MooneyeIntegrationTest {
    private static final String ROM_BASE_PATH = "/mooneye-tests/acceptance";
    private static final int MAX_CYCLES = 10_000_000; // 10 million cycles timeout

    private static final List<String> DISABLED_TESTS = Arrays.asList(
        "add_sp_e_timing",
        "boot_div-S",
        "boot_div-dmg0",
        "boot_div-dmgABCmgb",
        "boot_div2-S",
        "boot_hwio-S",
        "boot_hwio-dmg0",
        "boot_hwio-dmgABCmgb",
        "boot_regs-dmg0",
        "boot_sclk_align-dmgABCmgb",
        "call_cc_timing",
        "call_cc_timing2",
        "call_timing",
        "call_timing2",
        "di_timing-GS",
        "div_timing",
        "div_write",
        "ei_sequence",
        "ei_timing",
        "halt_ime0_nointr_timing",
        "halt_ime1_timing2-GS",
        "hblank_ly_scx_timing-GS",
        "ie_push",
        "if_ie_registers",
        "intr_1_2_timing-GS",
        "intr_2_0_timing",
        "intr_2_mode0_timing",
        "intr_2_mode0_timing_sprites",
        "intr_2_oam_ok_timing",
        "intr_timing",
        "jp_cc_timing",
        "jp_timing",
        "lcdon_timing-GS",
        "lcdon_write_timing-GS",
        "ld_hl_sp_e_timing",
        "oam_dma_restart",
        "oam_dma_start",
        "oam_dma_timing",
        "pop_timing",
        "push_timing",
        "rapid_di_ei",
        "rapid_toggle",
        "ret_cc_timing",
        "ret_timing",
        "reti_intr_timing",
        "reti_timing",
        "rst_timing",
        "sources-GS",
        "stat_irq_blocking",
        "stat_lyc_onoff",
        "tim00",
        "tim01",
        "tim01_div_trigger",
        "tim10",
        "tim10_div_trigger",
        "tim11",
        "tim11_div_trigger",
        "tima_reload",
        "tima_write_reloading",
        "tma_write_reloading",
        "unused_hwio-GS",
        "vblank_stat_intr-GS"
    );

    @ParameterizedTest(name = "{1}")
    @MethodSource("enabledTestProvider")
    void enabledMooneyeTests(String romPath, String testName) throws IOException {
        runMooneyeTest(romPath);
    }

    @Disabled("Enable individually as features are implemented")
    @ParameterizedTest(name = "{1}")
    @MethodSource("disabledTestProvider")
    void disabledMooneyeTests(String romPath, String testName) throws IOException {
        runMooneyeTest(romPath);
    }

    private void runMooneyeTest(String romPath) throws IOException {
        InputStream romStream = getClass().getResourceAsStream(romPath);
        assertNotNull(romStream, "Test ROM not found in resources: " + romPath);
        byte[] romData = romStream.readAllBytes();
        romStream.close();

        MooneyeTestRunner runner = new MooneyeTestRunner(romData);
        MooneyeTestRunner.RegisterState result = runner.runUntilCompletion(MAX_CYCLES);

        if (result.isSuccess()) {
            return;
        }

        if (result.isExpectedFailure()) {
            fail("Test ROM reported failure (registers all contain 0x42)");
        }

        fail(String.format(
            "Test ended in unexpected state. Expected success [3,5,8,13,21,34] or failure [42,42,42,42,42,42], got [%s] after %d cycles",
            result.toHexString(),
            result.cycles()
        ));
    }

    static Stream<Arguments> enabledTestProvider() {
        return discoverTests(testName -> !DISABLED_TESTS.contains(testName));
    }

    static Stream<Arguments> disabledTestProvider() {
        return discoverTests(DISABLED_TESTS::contains);
    }

    private static Stream<Arguments> discoverTests(java.util.function.Predicate<String> testFilter) {
        try {
            URI uri = MooneyeIntegrationTest.class.getResource(ROM_BASE_PATH).toURI();
            Path rootPath;

            if (uri.getScheme().equals("jar")) {
                FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                rootPath = fileSystem.getPath(ROM_BASE_PATH);
            } else {
                rootPath = Paths.get(uri);
            }

            return Files.walk(rootPath)
                .filter(path -> path.toString().endsWith(".gb"))
                .map(path -> new PathWithName(path, path.getFileName().toString()))
                .filter(MooneyeIntegrationTest::isDmgCompatible)
                .map(pwn -> {
                    String resourcePath = ROM_BASE_PATH + "/" + rootPath.relativize(pwn.path).toString().replace('\\', '/');
                    String testName = pwn.fileName.substring(0, pwn.fileName.lastIndexOf('.'));
                    return Arguments.of(resourcePath, testName);
                })
                .filter(args -> testFilter.test((String) args.get()[1]))
                .sorted(Comparator.comparing(a -> ((String) a.get()[1])));

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to discover Mooneye test ROMs", e);
        }
    }

    private static boolean isDmgCompatible(PathWithName pwn) {
        String nameWithoutExtension = pwn.fileName.substring(0, pwn.fileName.lastIndexOf('.'));

        return !(nameWithoutExtension.endsWith("-mgb")
                || nameWithoutExtension.endsWith("-sgb")
                || nameWithoutExtension.endsWith("-sgb2")
                || nameWithoutExtension.endsWith("-cgb")
                || nameWithoutExtension.endsWith("-agb"));
    }

    private record PathWithName(Path path, String fileName) {}
}
