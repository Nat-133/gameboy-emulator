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
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class MooneyeIntegrationTest {
    private static final String ROM_BASE_PATH = "/mooneye-tests/acceptance";
    private static final int MAX_CYCLES = 10_000_000; // 10 million cycles timeout

    private static final List<String> DISABLED_TESTS = Arrays.asList(
        "oam_dma_timing",
        "oam_dma_start",
        "oam_dma_restart",

        "stat_irq_blocking",
        "stat_lyc_onoff",
        "vblank_stat_intr-GS",

        "intr_2_0_timing",
        "intr_2_mode0_timing",
        "intr_2_mode0_timing_sprites",
        "intr_2_oam_ok_timing",
        "intr_1_2_timing-GS",
        "lcdon_timing-GS",
        "lcdon_write_timing-GS",
        "hblank_ly_scx_timing-GS",

        "halt_ime0_nointr_timing",
        "halt_ime1_timing2-GS",
        "reti_intr_timing",
        "reti_timing",
        "di_timing-GS",

        "call_timing",
        "call_timing2",
        "call_cc_timing",
        "call_cc_timing2",
        "jp_timing",
        "jp_cc_timing",
        "ret_timing",
        "ret_cc_timing",
        "rst_timing",
        "push_timing",
        "add_sp_e_timing",
        "ld_hl_sp_e_timing",

        "boot_div-dmgABCmgb",
        "boot_div-dmg0",
        "boot_div-S",
        "boot_div2-S",
        "boot_hwio-dmgABCmgb",
        "boot_hwio-dmg0",
        "boot_hwio-S",
        "boot_regs-dmg0",
        "unused_hwio-GS",
        "sources-GS",

        // Require t-cycle-level precision
        "ie_push",              // Requires cycle-accurate interrupt dispatch with IE sampling between push operations
        "rapid_toggle",         // Timer glitchy increments work but timing is off (40 vs 38 iterations) - needs instruction-level cycle accuracy
        "tma_write_reloading"   // Requires T-cycle precision for TMA sampling during TIMA reload phase
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
            fail("Test ROM reported failure (registers all contain 0x42) for test: " + romPath);
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
            URI uri = Objects.requireNonNull(MooneyeIntegrationTest.class.getResource(ROM_BASE_PATH)).toURI();
            Path rootPath;
            FileSystem fileSystem = null;

            if (uri.getScheme().equals("jar")) {
                fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                rootPath = fileSystem.getPath(ROM_BASE_PATH);
            } else {
                rootPath = Paths.get(uri);
            }

            List<Arguments> tests;
            try (Stream<Path> paths = Files.walk(rootPath)) {
                tests = paths
                    .filter(path -> path.toString().endsWith(".gb"))
                    .map(path -> new PathWithName(path, path.getFileName().toString()))
                    .filter(MooneyeIntegrationTest::isDmgCompatible)
                    .map(pwn -> {
                        String resourcePath = ROM_BASE_PATH + "/" + rootPath.relativize(pwn.path).toString().replace('\\', '/');
                        String testName = pwn.fileName.substring(0, pwn.fileName.lastIndexOf('.'));
                        return Arguments.of(resourcePath, testName);
                    })
                    .filter(args -> testFilter.test((String) args.get()[1]))
                    .sorted(Comparator.comparing(a -> ((String) a.get()[1])))
                    .toList();
            } finally {
                if (fileSystem != null) {
                    fileSystem.close();
                }
            }

            return tests.stream();

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
