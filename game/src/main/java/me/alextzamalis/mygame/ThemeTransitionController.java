package me.alextzamalis.mygame;

import me.alextzamalis.engine.graphics.Texture;
import me.alextzamalis.engine.graphics.WipeTransition;
import me.alextzamalis.mygame.data.BackgroundCatalog;
import me.alextzamalis.mygame.data.GameAudio;

/**
 * Orchestrates a 10-wave theme change with wipe, SFX, and music swap.
 */
public class ThemeTransitionController {

    private final WipeTransition wipe = new WipeTransition();

    private Texture oldBackground;
    private Texture newBackground;
    private Texture currentBackground;
    private int currentBlockIndex;
    private int targetBlockIndex;
    private Runnable onComplete;
    private boolean musicSwapped;
    private boolean running;

    public void initBlock(int blockIndex) {
        BackgroundCatalog.load();
        currentBlockIndex = blockIndex;
        currentBackground = BackgroundCatalog.getBackgroundForBlock(blockIndex);
        oldBackground = currentBackground;
        newBackground = currentBackground;
    }

    public Texture getCurrentBackground() {
        if (running && wipe.getProgress() >= 1f) {
            return newBackground != null ? newBackground : currentBackground;
        }
        return currentBackground;
    }

    public boolean isRunning() {
        return running;
    }

    public float getProgress() {
        return wipe.getProgress();
    }

    public void begin(int nextBlockIndex, Runnable onComplete) {
        this.targetBlockIndex = nextBlockIndex;
        this.onComplete = onComplete;
        this.oldBackground = currentBackground;
        this.newBackground = BackgroundCatalog.getBackgroundForBlock(nextBlockIndex);
        this.musicSwapped = false;
        this.running = true;

        GameAudio.playTransitionSfx();
        wipe.start(0.9f);
    }

    public void update(float dt) {
        if (!running) {
            return;
        }

        wipe.update(dt);

        if (!musicSwapped && wipe.getProgress() >= 0.45f) {
            String[] themes = BackgroundCatalog.getMusicThemesForBlock(targetBlockIndex);
            GameAudio.playRandomThemedMusic(themes);
            musicSwapped = true;
        }

        if (wipe.isFinished()) {
            currentBackground = newBackground;
            currentBlockIndex = targetBlockIndex;
            running = false;
            if (onComplete != null) {
                onComplete.run();
            }
            onComplete = null;
        }
    }

    public void renderWipe(me.alextzamalis.engine.graphics.BatchRenderer batch,
                           float x, float y, float width, float height) {
        if (!running) {
            return;
        }
        wipe.render(batch, oldBackground, newBackground, x, y, width, height);
    }
}
