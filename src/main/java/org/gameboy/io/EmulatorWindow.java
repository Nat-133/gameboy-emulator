package org.gameboy.io;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gameboy.common.Memory;
import org.gameboy.display.PpuRegisters;
import org.gameboy.io.debug.VramDebugWindow;

import javax.swing.*;
import java.awt.*;

public class EmulatorWindow {
    private final WindowDisplay windowDisplay;
    private final KeyboardInputHandler inputHandler;
    private final VramDebugWindow debugWindow;
    private JFrame frame;

    @Inject
    public EmulatorWindow(WindowDisplay windowDisplay,
                          KeyboardInputHandler inputHandler,
                          @Named("underlying") Memory memory,
                          PpuRegisters ppuRegisters) {
        this.windowDisplay = windowDisplay;
        this.inputHandler = inputHandler;
        this.debugWindow = new VramDebugWindow(memory, ppuRegisters);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Game Boy Emulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(500, 500);

            frame.add(windowDisplay, BorderLayout.CENTER);
            windowDisplay.addKeyListener(inputHandler);
            windowDisplay.setFocusable(true);

            frame.setVisible(true);
            windowDisplay.requestFocusInWindow();

            debugWindow.setLocation(frame.getX() + frame.getWidth() + 10, frame.getY());
            debugWindow.showWindow();
        });
    }

    public void refreshDebug() {
        SwingUtilities.invokeLater(debugWindow::refresh);
    }
}
