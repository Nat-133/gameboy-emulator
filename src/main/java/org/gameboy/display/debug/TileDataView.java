package org.gameboy.display.debug;

import org.gameboy.utils.MultiBitValue.TwoBitValue;

import javax.swing.*;
import java.awt.*;

public class TileDataView extends JPanel {
    private static final int TILE_SIZE = 8;
    private static final int TILES_PER_ROW = 16;
    private static final int TILE_ROWS = 24;
    private static final int PIXEL_SCALE = 2;
    private static final int TILE_SPACING = 1;

    private static final int PANEL_WIDTH = TILES_PER_ROW * (TILE_SIZE * PIXEL_SCALE + TILE_SPACING) + TILE_SPACING;
    private static final int PANEL_HEIGHT = TILE_ROWS * (TILE_SIZE * PIXEL_SCALE + TILE_SPACING) + TILE_SPACING;

    private TwoBitValue[][][] tileData;

    public TileDataView() {
        tileData = new TwoBitValue[TILES_PER_ROW * TILE_ROWS][TILE_SIZE][TILE_SIZE];
        for (int tile = 0; tile < TILES_PER_ROW * TILE_ROWS; tile++) {
            for (int row = 0; row < TILE_SIZE; row++) {
                for (int col = 0; col < TILE_SIZE; col++) {
                    tileData[tile][row][col] = TwoBitValue.b00;
                }
            }
        }

        Dimension size = new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setSize(size);
    }

    public void updateTileData(TwoBitValue[][][] newTileData) {
        this.tileData = newTileData;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        for (int tileIdx = 0; tileIdx < Math.min(tileData.length, TILES_PER_ROW * TILE_ROWS); tileIdx++) {
            int tileX = tileIdx % TILES_PER_ROW;
            int tileY = tileIdx / TILES_PER_ROW;

            int baseX = tileX * (TILE_SIZE * PIXEL_SCALE + TILE_SPACING);
            int baseY = tileY * (TILE_SIZE * PIXEL_SCALE + TILE_SPACING);

            for (int row = 0; row < TILE_SIZE; row++) {
                for (int col = 0; col < TILE_SIZE; col++) {
                    g2d.setColor(getColorForPixel(tileData[tileIdx][row][col]));
                    g2d.fillRect(
                        baseX + col * PIXEL_SCALE,
                        baseY + row * PIXEL_SCALE,
                        PIXEL_SCALE,
                        PIXEL_SCALE
                    );
                }
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
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }
}
