package org.gameboy.io;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.gameboy.common.Memory;
import org.gameboy.cpu.Cpu;
import org.gameboy.display.PpuRegisters;
import org.gameboy.io.debug.VramDebugWindow;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class EmulatorWindow {
    private static final int SCALE = 3;
    private static final int WINDOW_WIDTH = 160 * SCALE;
    private static final int WINDOW_HEIGHT = 144 * SCALE;

    private final WindowDisplay windowDisplay;
    private final KeyboardInputHandler inputHandler;
    private final VramDebugWindow debugWindow;
    private long window;

    @Inject
    public EmulatorWindow(WindowDisplay windowDisplay,
                          KeyboardInputHandler inputHandler,
                          @Named("underlying") Memory memory,
                          PpuRegisters ppuRegisters) {
        this.windowDisplay = windowDisplay;
        this.inputHandler = inputHandler;
        this.debugWindow = new VramDebugWindow(memory, ppuRegisters);
    }

    public void run(Cpu cpu) {
        init();

        System.out.println("Starting emulation...");

        try {
            while (!glfwWindowShouldClose(window)) {
                cpu.cycle();

                if (windowDisplay.needsRender()) {
                    glClear(GL_COLOR_BUFFER_BIT);
                    windowDisplay.render();
                    glfwSwapBuffers(window);
                    glfwPollEvents();

                    debugWindow.refresh();
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

        glViewport(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        inputHandler.registerCallbacks(window);
        windowDisplay.init();
        debugWindow.init(window);
    }

    private void cleanup() {
        windowDisplay.cleanup();
        debugWindow.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
