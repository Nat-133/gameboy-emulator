package org.gameboy.display;

import org.gameboy.Acid2TestRunner;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class Acid2IntegrationTest {
    private static final String ACID2_ROM_RESOURCE = "/dmg-acid2.gb";
    private static final String REFERENCE_IMAGE_RESOURCE = "/reference-dmg.png";
    private static final String OUTPUT_DIR = "build/test-outputs";
    private static final int TEST_TIMEOUT_CYCLES = 5_000_000;  // Enough for several frames

    private static final int GREYSCALE_WHITE = 0xFF;
    private static final int GREYSCALE_LIGHT = 0xAA;
    private static final int GREYSCALE_DARK = 0x55;
    private static final int GREYSCALE_BLACK = 0x00;

    @Test
    public void testAcid2DisplayOutput() throws IOException {
        // Load ROM from resources
        InputStream romStream = getClass().getResourceAsStream(ACID2_ROM_RESOURCE);
        assertNotNull(romStream, "dmg-acid2.gb ROM file not found in test resources: " + ACID2_ROM_RESOURCE);
        byte[] romData = romStream.readAllBytes();

        Acid2TestRunner runner = new Acid2TestRunner(romData);

        BufferedImage actualImage = runner.runUntilStableAndCapture(TEST_TIMEOUT_CYCLES);
        assertNotNull(actualImage, "Failed to capture screenshot from emulator");

        // Create output directory if it doesn't exist
        Path outputPath = Path.of(OUTPUT_DIR);
        Files.createDirectories(outputPath);

        File outputFile = outputPath.resolve("acid2-test-output.png").toFile();
        ImageIO.write(actualImage, "png", outputFile);
        System.out.println("Test screenshot saved to: " + outputFile.getAbsolutePath());

        // Load reference image from resources
        InputStream referenceStream = getClass().getResourceAsStream(REFERENCE_IMAGE_RESOURCE);
        assertNotNull(referenceStream, "Reference image not found in resources: " + REFERENCE_IMAGE_RESOURCE);
        BufferedImage groundTruthImage = ImageIO.read(referenceStream);

        assertImagesMatch(groundTruthImage, actualImage);
    }

    private void assertImagesMatch(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth(), "Image widths don't match");
        assertEquals(expected.getHeight(), actual.getHeight(), "Image heights don't match");

        int differences = 0;
        StringBuilder mismatchDetails = new StringBuilder();

        for (int x = 0; x < expected.getWidth(); x++) {
            for (int y = 0; y < expected.getHeight(); y++) {
                int expectedRGB = expected.getRGB(x, y);
                int actualRGB = actual.getRGB(x, y);

                int expectedGrey = (expectedRGB >> 16) & 0xFF;
                int actualGrey = (actualRGB >> 16) & 0xFF;

                if (!isValidDMGGreyscale(actualGrey)) {
                    fail(String.format("Invalid greyscale value at (%d, %d): 0x%02X. " +
                                     "Must be one of: 0x00, 0x55, 0xAA, 0xFF",
                                     x, y, actualGrey));
                }

                if (expectedGrey != actualGrey) {
                    differences++;
                    if (differences <= 10) {
                        mismatchDetails.append(String.format(
                            "Pixel (%d, %d): expected=0x%02X, actual=0x%02X\n",
                            x, y, expectedGrey, actualGrey
                        ));
                    }
                }
            }
        }

        if (differences > 0) {
            fail(String.format("Images don't match. %d pixels differ (%.2f%%):\n%s",
                differences,
                (differences * 100.0) / (expected.getWidth() * expected.getHeight()),
                    mismatchDetails));
        }
    }

    private boolean isValidDMGGreyscale(int value) {
        return value == GREYSCALE_BLACK || value == GREYSCALE_DARK ||
               value == GREYSCALE_LIGHT || value == GREYSCALE_WHITE;
    }
}