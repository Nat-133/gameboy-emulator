package org.gameboy.io.debug;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

import javax.swing.*;
import java.awt.*;

public class TileMapView extends JPanel {
    private static final int MAP_SIZE = 256;
    private static final int PIXEL_SCALE = 2;

    private TwoBitValue[][] tileMapData;
    private Rectangle viewport;
    private Color viewportColor = new Color(255, 0, 0, 128);

    public TileMapView() {
        tileMapData = new TwoBitValue[MAP_SIZE][MAP_SIZE];
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                tileMapData[y][x] = TwoBitValue.b00;
            }
        }
    }

    public void updateTileMap(TwoBitValue[][] newData) {
        this.tileMapData = newData;
        repaint();
    }

    public void setViewport(int x, int y, int width, int height) {
        this.viewport = new Rectangle(x, y, width, height);
        repaint();
    }

    public void clearViewport() {
        this.viewport = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                g2d.setColor(getColorForPixel(tileMapData[y][x]));
                g2d.fillRect(x * PIXEL_SCALE, y * PIXEL_SCALE, PIXEL_SCALE, PIXEL_SCALE);
            }
        }
    }

    private Color getColorForPixel(TwoBitValue value) {
        return switch (value) {
            case b00 -> new Color(224, 248, 208);
            case b01 -> new Color(136, 192, 70);
            case b10 -> new Color(52, 104, 50);
            case b11 -> new Color(8, 24, 32);
        };
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(MAP_SIZE * PIXEL_SCALE, MAP_SIZE * PIXEL_SCALE);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
