package org.gameboy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import org.gameboy.common.MappedMemory;
import org.gameboy.common.Memory;
import org.gameboy.common.MemoryDump;
import org.gameboy.cpu.Cpu;
import org.gameboy.display.Display;
import org.gameboy.display.PixelBuffer;
import org.gameboy.display.PixelValue;

import java.awt.image.BufferedImage;
import java.util.List;

public class Acid2TestRunner {
    public static final int FRAME_COUNT = 60;
    private final Cpu cpu;
    private final TestDisplay display;
    private int cycleCount = 0;

    public Acid2TestRunner(byte[] romData) {
        display = new TestDisplay();

        AbstractModule testOverrideModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(Display.class).toInstance(display);
            }
        };

        Injector injector = Guice.createInjector(
            Modules.override(new EmulatorModule()).with(testOverrideModule)
        );

        List<MemoryDump> memoryDumps = new java.util.ArrayList<>();
        memoryDumps.add(MemoryDump.fromZero(romData));

        Memory underlyingMemory = injector.getInstance(Key.get(Memory.class, Names.named("underlying")));
        if (underlyingMemory instanceof MappedMemory mappedMemory) {
            mappedMemory.loadMemoryDumps(memoryDumps);
        }

        cpu = injector.getInstance(Cpu.class);
    }

    public BufferedImage runUntilStableAndCapture(int maxCycles) {

        while (cycleCount < maxCycles && display.getFrameCount() < FRAME_COUNT) {
            cpu.cycle();
            cycleCount++;
        }

        if (display.getFrameCount() == 0) {
            System.err.println("Warning: No frames rendered after " + cycleCount + " cycles");
            return null;
        }

        return display.captureScreenshot();
    }

    private static class TestDisplay implements Display {
        private final PixelBuffer pixelBuffer;
        private final int[] greyValues;
        private int frameCount = 0;

        private static final int[] DMG_GREYSCALE = {0xFF, 0xAA, 0x55, 0x00};

        public TestDisplay() {
            this(DMG_GREYSCALE[0], DMG_GREYSCALE[1], DMG_GREYSCALE[2], DMG_GREYSCALE[3]);
        }

        public TestDisplay(int grey0, int grey1, int grey2, int grey3) {
            this.pixelBuffer = new PixelBuffer();
            this.greyValues = new int[]{grey0, grey1, grey2, grey3};
        }

        @Override
        public void setPixel(int x, int y, PixelValue value) {
            pixelBuffer.setPixel(x, y, value);
        }

        @Override
        public void onVBlank() {
            frameCount++;
            pixelBuffer.swapBuffers();
        }

        public int getFrameCount() {
            return frameCount;
        }

        public BufferedImage captureScreenshot() {
            BufferedImage image = new BufferedImage(DISPLAY_WIDTH, DISPLAY_HEIGHT, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < DISPLAY_WIDTH; x++) {
                for (int y = 0; y < DISPLAY_HEIGHT; y++) {
                    PixelValue pixel = pixelBuffer.getDisplayPixel(x, y);
                    int greyValue = greyValues[pixel.value() & 0x3];

                    int rgb = (greyValue << 16) | (greyValue << 8) | greyValue;
                    image.setRGB(x, y, rgb);
                }
            }

            return image;
        }
    }
}