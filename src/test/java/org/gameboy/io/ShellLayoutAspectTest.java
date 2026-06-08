package org.gameboy.io;

import org.gameboy.display.Display;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies that the rendered Game Boy display region in the shell layout has the
 * correct 160:144 aspect ratio.
 *
 * The rendered aspect depends on:
 *   - The viewport aspect (after letterboxing, this equals WINDOW_WIDTH / WINDOW_HEIGHT)
 *   - The screen region in shell normalized coordinates
 *   - VIEW_SCALE (which compresses the visible normY range to [0, VIEW_SCALE])
 *
 * The vertex shader maps normalized layout coords to NDC as:
 *   ndcX = normX * 2 - 1                      // 1 normX unit  = viewportW pixels
 *   ndcY = 1 - (normY / VIEW_SCALE) * 2       // 1 normY unit = viewportH / VIEW_SCALE pixels
 *
 * So the rendered pixel aspect of the screen region is:
 *   (screenWidthNorm  * viewportW) /
 *   (screenHeightNorm * viewportH / VIEW_SCALE)
 *   = (screenWidthNorm / screenHeightNorm) * VIEW_SCALE * (viewportW / viewportH)
 */
class ShellLayoutAspectTest {

    private static final float GB_NATIVE_ASPECT = (float) Display.DISPLAY_WIDTH / Display.DISPLAY_HEIGHT;

    private static float renderedScreenAspect() {
        float screenWidthNorm = ShellLayout.SCREEN_RIGHT - ShellLayout.SCREEN_LEFT;
        float screenHeightNorm = ShellLayout.SCREEN_BOTTOM - ShellLayout.SCREEN_TOP;
        float viewportAspect = (float) ShellLayout.WINDOW_WIDTH / ShellLayout.WINDOW_HEIGHT;
        return (screenWidthNorm / screenHeightNorm) * ShellLayout.VIEW_SCALE * viewportAspect;
    }

    @Test
    void screenRegion_rendersAtGameBoyNativeAspectRatio() {
        assertThat(renderedScreenAspect())
                .as("Game screen region should render at native 160:144 aspect; "
                        + "if this fails the GB display will appear stretched or squished")
                .isCloseTo(GB_NATIVE_ASPECT, within(0.01f));
    }

    @Test
    void bezelRegion_isWiderThanScreenRegion() {
        float bezelW = ShellLayout.BEZEL_RIGHT - ShellLayout.BEZEL_LEFT;
        float bezelH = ShellLayout.BEZEL_BOTTOM - ShellLayout.BEZEL_TOP;
        float screenW = ShellLayout.SCREEN_RIGHT - ShellLayout.SCREEN_LEFT;
        float screenH = ShellLayout.SCREEN_BOTTOM - ShellLayout.SCREEN_TOP;

        assertThat(bezelW).as("bezel width").isGreaterThan(screenW);
        assertThat(bezelH).as("bezel height").isGreaterThan(screenH);
    }
}
