package org.gameboy.display;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WindowDisplay extends JPanel implements Display {
    private final PixelBuffer pixelBuffer;
    private final Color[] colors;
    private static final int PIXEL_SIZE = 2;
    private static final boolean SAVE_SCREENSHOTS = false;
    private static int frameCounter = 0;

    public WindowDisplay(Color color0, Color color1, Color color2, Color color3) {
        this.pixelBuffer = new PixelBuffer();
        this.colors = new Color[]{color0, color1, color2, color3};
    }

    @Override
    public void setPixel(int x, int y, PixelValue value) {
        pixelBuffer.setPixel(x, y, value);
    }

    @Override
    public void onVBlank() {
        frameCounter++;
        pixelBuffer.swapBuffers();

        if (SAVE_SCREENSHOTS && frameCounter >= 60 && frameCounter < 70) {
            saveScreenshot();
        }

        SwingUtilities.invokeLater(this::repaint);
    }

    private void saveScreenshot() {
        BufferedImage image = new BufferedImage(DISPLAY_WIDTH, DISPLAY_HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < DISPLAY_WIDTH; x++) {
            for (int y = 0; y < DISPLAY_HEIGHT; y++) {
                Color color = getColorForPixel(pixelBuffer.getDisplayPixel(x, y));
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
        return colors[value.value() & 0x3];
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.fillRect(0,100,50,50);

        for (int x = 0; x < DISPLAY_WIDTH; x++){
            for (int y = 0; y < DISPLAY_HEIGHT; y++) {
                g2d.setColor(getColorForPixel(pixelBuffer.getDisplayPixel(x, y)));
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
