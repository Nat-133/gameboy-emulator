package org.gameboy.io;

import org.gameboy.components.joypad.MultiSourceButton;
import org.gameboy.io.shell.ShellElement;

import java.util.ArrayList;
import java.util.List;

import static org.gameboy.io.ShellLayout.*;
import static org.lwjgl.glfw.GLFW.*;

public class MouseInputHandler {
    private static final String SOURCE = "mouse";

    private List<ShellElement> interactiveElements = List.of();
    private final List<MultiSourceButton> mouseHeld = new ArrayList<>();
    private double mouseX;
    private double mouseY;
    private boolean mousePressed;

    public void setInteractiveElements(List<ShellElement> elements) {
        this.interactiveElements = elements;
    }

    public void registerCallbacks(long window) {
        glfwSetMouseButtonCallback(window, this::handleMouseButton);
        glfwSetCursorPosCallback(window, this::handleCursorPos);
    }

    private void handleMouseButton(long window, int button, int action, int mods) {
        if (button != GLFW_MOUSE_BUTTON_LEFT) return;

        if (action == GLFW_PRESS) {
            mousePressed = true;
            updateHeldButtons();
        } else if (action == GLFW_RELEASE) {
            mousePressed = false;
            releaseAllHeld();
        }
    }

    private void handleCursorPos(long window, double xpos, double ypos) {
        mouseX = xpos / WINDOW_WIDTH;
        mouseY = (ypos / WINDOW_HEIGHT) * VIEW_SCALE;

        if (mousePressed) {
            updateHeldButtons();
        }
    }

    private void updateHeldButtons() {
        float mx = (float) mouseX;
        float my = (float) mouseY;

        // Release buttons no longer under cursor
        List<MultiSourceButton> toRelease = new ArrayList<>();
        for (MultiSourceButton btn : mouseHeld) {
            if (!isOverAnyElement(mx, my, btn)) {
                toRelease.add(btn);
            }
        }
        for (MultiSourceButton btn : toRelease) {
            btn.release(SOURCE);
            mouseHeld.remove(btn);
        }

        // Check all interactive elements for hits
        for (ShellElement element : interactiveElements) {
            MultiSourceButton hit = element.hitTest(mx, my);
            if (hit != null && !mouseHeld.contains(hit)) {
                hit.press(SOURCE);
                mouseHeld.add(hit);
            }
        }
    }

    private boolean isOverAnyElement(float mx, float my, MultiSourceButton target) {
        for (ShellElement element : interactiveElements) {
            MultiSourceButton hit = element.hitTest(mx, my);
            if (hit == target) return true;
        }
        return false;
    }

    private void releaseAllHeld() {
        for (MultiSourceButton btn : mouseHeld) {
            btn.release(SOURCE);
        }
        mouseHeld.clear();
    }
}
