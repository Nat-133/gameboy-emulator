package org.gameboy.display.debug;

import org.gameboy.common.Memory;
import org.gameboy.display.LcdcParser;
import org.gameboy.display.PpuRegisters;

import javax.swing.*;
import java.awt.*;

import static org.gameboy.display.Display.DISPLAY_HEIGHT;
import static org.gameboy.display.Display.DISPLAY_WIDTH;
import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.gameboy.utils.BitUtilities.uint;

/**
 * Debug window for visualizing VRAM contents including:
 * - Background tile map with viewport overlay
 * - Window tile map with viewport overlay
 */
public class VramDebugWindow extends JFrame {
    private final TileRenderer tileRenderer;
    private final PpuRegisters registers;
    private final Memory memory;

    private final TileMapView backgroundView;
    private final TileMapView windowView;
    private final TileDataView tileDataView;
    private final JLabel registerInfoLabel;
    private final JLabel tileDataLabel;

    public VramDebugWindow(Memory memory, PpuRegisters registers) {
        super("VRAM Debug Viewer");

        this.memory = memory;
        this.registers = registers;
        this.tileRenderer = new TileRenderer(memory);

        backgroundView = new TileMapView();
        windowView = new TileMapView();
        tileDataView = new TileDataView();

        registerInfoLabel = new JLabel();
        registerInfoLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        tileDataLabel = new JLabel("Tile Data (0x8000-0x97FF)");
        tileDataLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tileDataLabel.setHorizontalAlignment(SwingConstants.CENTER);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Background Tile Map"), gbc);

        gbc.gridy = 1;
        add(backgroundView, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        add(new JLabel("Window Tile Map"), gbc);

        gbc.gridy = 1;
        add(windowView, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(registerInfoLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(tileDataLabel, gbc);

        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.BOTH;
        add(tileDataView, gbc);

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    public void refresh() {
        byte lcdc = registers.read(LCDC);
        boolean useSigned = !LcdcParser.useUnsignedTileDataSelect(lcdc);

        int backgroundTileMap = LcdcParser.backgroundTileMap(lcdc);
        int windowTileMap = LcdcParser.windowTileMap(lcdc);

        var backgroundData = tileRenderer.renderTileMap(backgroundTileMap, useSigned);
        backgroundView.updateTileMap(backgroundData);

        int scx = uint(registers.read(SCX));
        int scy = uint(registers.read(SCY));
        backgroundView.setViewport(scx, scy, DISPLAY_WIDTH, DISPLAY_HEIGHT);

        var windowData = tileRenderer.renderTileMap(windowTileMap, useSigned);
        windowView.updateTileMap(windowData);

        int wx = uint(registers.read(WX));
        int wy = uint(registers.read(WY));

        int windowScreenX = wx - 7;
        int windowScreenY = wy;

        if (windowScreenX < DISPLAY_WIDTH && windowScreenY < DISPLAY_HEIGHT) {
            int visibleWidth = Math.min(DISPLAY_WIDTH - windowScreenX, DISPLAY_WIDTH);
            int visibleHeight = Math.min(DISPLAY_HEIGHT - windowScreenY, DISPLAY_HEIGHT);

            if (visibleWidth > 0 && visibleHeight > 0) {
                windowView.setViewport(0, 0, visibleWidth, visibleHeight);
            } else {
                windowView.clearViewport();
            }
        } else {
            windowView.clearViewport();
        }

        var allTiles = tileRenderer.renderAllTiles();
        tileDataView.updateTileData(allTiles);

        updateRegisterInfo(scx, scy, wx, wy);
    }

    private void updateRegisterInfo(int scx, int scy, int wx, int wy) {
        String info = String.format(
            "<html>Background Scroll: SCX=$%02X (%d), SCY=$%02X (%d) | " +
            "Window Position: WX=$%02X (%d), WY=$%02X (%d)</html>",
            scx, scx, scy, scy, wx, wx, wy, wy
        );
        registerInfoLabel.setText(info);
    }

    public void showWindow() {
        setVisible(true);
        refresh();
    }
}
