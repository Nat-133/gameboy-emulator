package org.gameboy.io;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gameboy.common.Memory;
import org.gameboy.components.joypad.MultiSourceButton;
import org.gameboy.components.joypad.annotations.*;
import org.gameboy.cpu.Cpu;
import org.gameboy.display.PpuRegisters;
import org.gameboy.io.debug.VramDebugWindow;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class EmulatorWindow {
    private static final int WINDOW_WIDTH = ShellLayout.WINDOW_WIDTH;
    private static final int WINDOW_HEIGHT = ShellLayout.WINDOW_HEIGHT;

    private final WindowDisplay windowDisplay;
    private final KeyboardInputHandler inputHandler;
    private final MouseInputHandler mouseInputHandler;
    private final GameBoyShell gameBoyShell;
    private final VramDebugWindow debugWindow;

    private final MultiSourceButton up, down, left, right, a, b, start, select;

    // Button animation states: up, down, left, right, a, b, start, select
    private final ButtonAnimationState[] buttonAnimations;
    private final float[] buttonStates = new float[8];

    private long window;
    private double lastTime;

    @Inject
    public EmulatorWindow(WindowDisplay windowDisplay,
                          KeyboardInputHandler inputHandler,
                          MouseInputHandler mouseInputHandler,
                          GameBoyShell gameBoyShell,
                          @Named("underlying") Memory memory,
                          PpuRegisters ppuRegisters,
                          @ButtonUp MultiSourceButton up,
                          @ButtonDown MultiSourceButton down,
                          @ButtonLeft MultiSourceButton left,
                          @ButtonRight MultiSourceButton right,
                          @ButtonA MultiSourceButton a,
                          @ButtonB MultiSourceButton b,
                          @ButtonStart MultiSourceButton start,
                          @ButtonSelect MultiSourceButton select) {
        this.windowDisplay = windowDisplay;
        this.inputHandler = inputHandler;
        this.mouseInputHandler = mouseInputHandler;
        this.gameBoyShell = gameBoyShell;
        this.debugWindow = new VramDebugWindow(memory, ppuRegisters);

        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
        this.a = a;
        this.b = b;
        this.start = start;
        this.select = select;

        this.buttonAnimations = new ButtonAnimationState[]{
            new ButtonAnimationState(up),
            new ButtonAnimationState(down),
            new ButtonAnimationState(left),
            new ButtonAnimationState(right),
            new ButtonAnimationState(a),
            new ButtonAnimationState(b),
            new ButtonAnimationState(start),
            new ButtonAnimationState(select),
        };
    }

    public void run(Cpu cpu) {
        init();

        System.out.println("Starting emulation...");

        lastTime = glfwGetTime();
        double lastRenderTime = lastTime;

        try {
            while (!glfwWindowShouldClose(window)) {
                cpu.cycle();

                // Update game texture only when a new frame is ready (VBlank)
                if (windowDisplay.needsRender()) {
                    windowDisplay.updateTexture();
                    debugWindow.refresh();
                }

                // Render the shell at ~60Hz for smooth button animations
                double now = glfwGetTime();
                double elapsed = now - lastRenderTime;
                if (elapsed >= 1.0 / 60.0) {
                    float deltaTime = (float) elapsed;
                    lastRenderTime = now;

                    // Update button animations
                    for (int i = 0; i < 8; i++) {
                        buttonAnimations[i].update(deltaTime);
                        buttonStates[i] = buttonAnimations[i].getProgress();
                    }

                    glClear(GL_COLOR_BUFFER_BIT);
                    gameBoyShell.render(windowDisplay.getTextureId(), (float) now, buttonStates);
                    glfwSwapBuffers(window);
                    glfwPollEvents();
                }
            }
        } finally {
            cleanup();
        }
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Game Boy Emulator", NULL, NULL);
        if (window == NULL) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Use framebuffer size for viewport (handles HiDPI)
        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        glfwGetFramebufferSize(window, fbWidth, fbHeight);
        glViewport(0, 0, fbWidth[0], fbHeight[0]);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        inputHandler.registerCallbacks(window);
        windowDisplay.init();
        gameBoyShell.init(up, down, left, right, a, b, start, select);
        mouseInputHandler.setInteractiveElements(gameBoyShell.getInteractiveElements());
        mouseInputHandler.registerCallbacks(window);
        debugWindow.init(window);
    }

    private void cleanup() {
        windowDisplay.cleanup();
        gameBoyShell.cleanup();
        debugWindow.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
