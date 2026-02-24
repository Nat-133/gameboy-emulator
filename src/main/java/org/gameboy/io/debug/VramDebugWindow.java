package org.gameboy.io.debug;

import org.gameboy.common.Memory;
import org.gameboy.display.LcdcParser;
import org.gameboy.display.PpuRegisters;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.gameboy.display.Display.DISPLAY_HEIGHT;
import static org.gameboy.display.Display.DISPLAY_WIDTH;
import static org.gameboy.display.PpuRegisters.PpuRegister.*;
import static org.gameboy.utils.BitUtilities.uint;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class VramDebugWindow {
    private static final int TILE_MAP_DISPLAY_SIZE = 512;  // 256 * 2
    private static final int GAP = 10;
    private static final int TILE_DATA_DISPLAY_WIDTH = 512;   // 256 * 2
    private static final int TILE_DATA_DISPLAY_HEIGHT = 192;  // 96 * 2

    private static final int WINDOW_WIDTH = TILE_MAP_DISPLAY_SIZE * 2 + GAP;
    private static final int WINDOW_HEIGHT = TILE_MAP_DISPLAY_SIZE + GAP + TILE_DATA_DISPLAY_HEIGHT;

    private final TileRenderer tileRenderer;
    private final PpuRegisters registers;

    private final TileMapView backgroundView;
    private final TileMapView windowView;
    private final TileDataView tileDataView;

    private long window;
    private long mainWindow;
    private int shaderProgram;
    private int vao;
    private int vbo;
    private boolean visible;

    public VramDebugWindow(Memory memory, PpuRegisters registers) {
        this.registers = registers;
        this.tileRenderer = new TileRenderer(memory);
        this.backgroundView = new TileMapView();
        this.windowView = new TileMapView();
        this.tileDataView = new TileDataView();
    }

    public void init(long mainWindow) {
        this.mainWindow = mainWindow;

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT,
                "VRAM Debug Viewer", NULL, mainWindow);
        if (window == NULL) {
            System.err.println("Failed to create debug window");
            return;
        }

        // Position debug window next to main window
        int[] mainX = new int[1], mainY = new int[1];
        int[] mainW = new int[1];
        glfwGetWindowPos(mainWindow, mainX, mainY);
        glfwGetWindowSize(mainWindow, mainW, new int[1]);
        glfwSetWindowPos(window, mainX[0] + mainW[0] + 10, mainY[0]);

        // Initialize GL resources in debug window context
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

        shaderProgram = createShaderProgram();
        createQuad();

        backgroundView.init();
        windowView.init();
        tileDataView.init();

        visible = true;

        // Restore main window context
        glfwMakeContextCurrent(mainWindow);
    }

    public void refresh() {
        if (window == NULL || !visible) return;

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

        render();
    }

    private void render() {
        glfwMakeContextCurrent(window);

        glClear(GL_COLOR_BUFFER_BIT);

        // Background tile map (top-left)
        glViewport(0, GAP + TILE_DATA_DISPLAY_HEIGHT,
                TILE_MAP_DISPLAY_SIZE, TILE_MAP_DISPLAY_SIZE);
        backgroundView.render(shaderProgram, vao);

        // Window tile map (top-right)
        glViewport(TILE_MAP_DISPLAY_SIZE + GAP, GAP + TILE_DATA_DISPLAY_HEIGHT,
                TILE_MAP_DISPLAY_SIZE, TILE_MAP_DISPLAY_SIZE);
        windowView.render(shaderProgram, vao);

        // Tile data (bottom, centered)
        glViewport(0, 0, TILE_DATA_DISPLAY_WIDTH, TILE_DATA_DISPLAY_HEIGHT);
        tileDataView.render(shaderProgram, vao);

        glfwSwapBuffers(window);

        // Restore main window context
        glfwMakeContextCurrent(mainWindow);
    }

    public void cleanup() {
        if (window == NULL) return;

        glfwMakeContextCurrent(window);

        backgroundView.cleanup();
        windowView.cleanup();
        tileDataView.cleanup();
        glDeleteProgram(shaderProgram);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);

        glfwMakeContextCurrent(mainWindow);
        glfwDestroyWindow(window);
    }

    private int createShaderProgram() {
        String vertexSource = loadShaderResource("shaders/vertex.glsl");
        String fragmentSource = loadShaderResource("shaders/fragment.glsl");

        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Debug shader program linking failed: " + log);
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException("Debug shader compilation failed: " + log);
        }

        return shader;
    }

    private String loadShaderResource(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }

    private void createQuad() {
        float[] vertices = {
            -1.0f,  1.0f,  0.0f, 0.0f,
            -1.0f, -1.0f,  0.0f, 1.0f,
             1.0f,  1.0f,  1.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 1.0f,
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        ByteBuffer vertexBuffer = BufferUtils.createByteBuffer(vertices.length * Float.BYTES);
        for (float v : vertices) {
            vertexBuffer.putFloat(v);
        }
        vertexBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }
}
