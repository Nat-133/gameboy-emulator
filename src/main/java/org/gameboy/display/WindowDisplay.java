package org.gameboy.display;

import org.gameboy.utils.MultiBitValue;

import javax.swing.*;
import java.awt.*;

public class WindowDisplay extends JPanel implements Display {
    PixelValue[][] pixels = new PixelValue[DISPLAY_WIDTH][DISPLAY_HEIGHT];
    private static final int PIXEL_SIZE = 2;

    public WindowDisplay() {
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[i].length; j++) {
                pixels[i][j] = new PixelValue((i+j)%2);
            }
        }
    }

    @Override
    public void setPixel(int x, int y, PixelValue value) {
        this.pixels[x][y] = value;
        this.repaint();
        this.revalidate();
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

        for (int x = 0; x < pixels.length; x++){
            for (int y = 0; y < pixels[x].length; y++) {
                g2d.setColor(getColorForPixel(pixels[x][y]));
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
