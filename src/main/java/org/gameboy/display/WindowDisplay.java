package org.gameboy.display;

import org.gameboy.utils.MultiBitValue;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WindowDisplay extends JPanel implements Display {
    PixelValue[][] pixels = new PixelValue[DISPLAY_WIDTH][DISPLAY_HEIGHT];
    private final PixelValue[][] displayBuffer = new PixelValue[DISPLAY_WIDTH][DISPLAY_HEIGHT];
    private static final int PIXEL_SIZE = 2;
    private static final boolean SAVE_SCREENSHOTS = false;
    private static int frameCounter = 0;

    public WindowDisplay() {
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                pixels[i][j] = new PixelValue((i+j)%2);
                displayBuffer[i][j] = new PixelValue((i+j)%2);
            }
        }
    }

    @Override
    public void setPixel(int x, int y, PixelValue value) {
        if (x < DISPLAY_WIDTH && y < DISPLAY_HEIGHT && x >= 0 && y >= 0) {
            this.pixels[x][y] = value;
            // Don't repaint here - wait for VBlank to repaint once per frame
        }
    }

    @Override
    public void onVBlank() {
        frameCounter++;

        for (int x = 0; x < DISPLAY_WIDTH; x++) {
            System.arraycopy(pixels[x], 0, displayBuffer[x], 0, DISPLAY_HEIGHT);
        }

        if (SAVE_SCREENSHOTS && frameCounter >= 60 && frameCounter < 70) {
            saveScreenshot();
        }

        SwingUtilities.invokeLater(this::repaint);
    }

    private void saveScreenshot() {
        BufferedImage image = new BufferedImage(DISPLAY_WIDTH, DISPLAY_HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < DISPLAY_WIDTH; x++) {
            for (int y = 0; y < DISPLAY_HEIGHT; y++) {
                Color color = getColorForPixel(displayBuffer[x][y]);
                image.setRGB(x, y, color.getRGB());
            }
        }

        try {
            File outputFile = new File(String.format("frame_%04d.png", frameCounter));
            ImageIO.write(image, "png", outputFile);
            System.out.printf("Saved screenshot: %s%n", outputFile.getName());
        } catch (IOException e) {
            System.err.printf("Failed to save screenshot: %s%n", e.getMessage());
        }
    }

    private Color getColorForPixel(PixelValue value) {
        return switch(MultiBitValue.TwoBitValue.from(value.value())) {
            case b00 -> new Color(224, 248, 208);
            case b01 -> new Color(136, 192, 70);
            case b10 -> new Color(52, 104, 50);
            case b11 -> new Color(8, 24, 32);
        };
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.fillRect(0,100,50,50);

        for (int x = 0; x < displayBuffer.length; x++){
            for (int y = 0; y < displayBuffer[x].length; y++) {
                g2d.setColor(getColorForPixel(displayBuffer[x][y]));
                g2d.fillRect(x*PIXEL_SIZE, y*PIXEL_SIZE, PIXEL_SIZE, PIXEL_SIZE);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PIXEL_SIZE * DISPLAY_WIDTH,PIXEL_SIZE * DISPLAY_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}
