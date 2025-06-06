package org.gameboy.display;

public class PixelFetcher {
    private boolean paused;
    private final BackgroundFetcher backgroundFetcher;
    private final SpriteFetcher spriteFetcher;
    private Fetcher activeFetcher;

    public PixelFetcher(BackgroundFetcher backgroundFetcher, SpriteFetcher spriteFetcher) {
        this.backgroundFetcher = backgroundFetcher;
        this.spriteFetcher = spriteFetcher;
        activeFetcher = backgroundFetcher;
    }

    public void tick() {
        if (paused) {
            return;
        }

        activeFetcher.runSingleTickCycle();

        if (spriteFetcher.fetchComplete()) {
            activeFetcher = backgroundFetcher;
        }
    }

    public void start() {
        paused = false;
        backgroundFetcher.reset();
    }

    public void stop() {
        paused = true;
    }

    public void fetchSprite() {
        activeFetcher = spriteFetcher;
    }
}
