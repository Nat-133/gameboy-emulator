package org.gameboy.io.shell;

import org.gameboy.components.joypad.MultiSourceButton;
import org.gameboy.io.ShellLayout;

import static org.lwjgl.opengl.GL41.*;

public abstract sealed class ShellElement permits
        BackgroundElement,
        RoundedRectElement,
        GameScreenElement,
        DpadElement,
        CircleButtonElement,
        PillButtonElement,
        BitmapTextElement {

    private static final String VERTEX_PATH = "shaders/shell/shell_quad_vertex.glsl";

    protected final float x, y, width, height;
    protected ShaderProgram shader;

    // Cached uniform locations
    private int uElementBoundsLoc;
    private int uViewScaleLoc;

    protected ShellElement(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    protected abstract String getFragmentShaderPath();

    public void init(ShaderCache shaderCache) {
        shader = shaderCache.get(VERTEX_PATH, getFragmentShaderPath());
        uElementBoundsLoc = shader.getUniformLocation("uElementBounds");
        uViewScaleLoc = shader.getUniformLocation("uViewScale");
        initUniforms();
    }

    protected void initUniforms() {
        // Subclasses cache their uniform locations here
    }

    public final void render(QuadRenderer renderer, float aspect) {
        shader.use();
        shader.setUniform4f(uElementBoundsLoc, x, y, width, height);
        shader.setUniform1f(uViewScaleLoc, ShellLayout.VIEW_SCALE);
        setElementUniforms(aspect);
        renderer.drawQuad();
    }

    protected abstract void setElementUniforms(float aspect);

    public MultiSourceButton hitTest(float mx, float my) {
        return null;
    }
}
