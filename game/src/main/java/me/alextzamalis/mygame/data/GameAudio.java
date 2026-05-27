package me.alextzamalis.mygame.data;

import me.alextzamalis.engine.audio.AudioManager;
import me.alextzamalis.engine.audio.MusicTrack;
import me.alextzamalis.engine.audio.Sound;
import me.alextzamalis.engine.core.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game audio paths and helpers.
 */
public final class GameAudio {

    public static final String SFX_SHOOT = "/audio/sfx/shoot.wav";
    public static final String SFX_HIT = "/audio/sfx/hit.wav";
    public static final String SFX_TRANSITION = "/audio/sfx/trasnsition-sfx1.wav";
    public static final String MUSIC_MENU = "/audio/music/themed/Various Themes/Waiting.ogg";

    private static final String[] UI_CLICK_PATHS = {
            "/audio/sfx/interface/tonal-interface-001.wav",
            "/audio/sfx/interface/tonal-interface-002.wav",
            "/audio/sfx/interface/tonal-interface-003.wav",
            "/audio/sfx/interface/tonal-interface-004.wav",
            "/audio/sfx/interface/tonal-interface-005.wav",
            "/audio/sfx/interface/tonal-interface-006.wav"
    };

    private static Sound shootSfx;
    private static Sound hitSfx;
    private static Sound transitionSfx;
    private static final List<Sound> uiClickSfx = new ArrayList<>();
    private static MusicTrack menuMusic;
    private static MusicTrack currentMusic;
    private static final Random rng = new Random();

    private GameAudio() {
    }

    public static void load() {
        if (!AudioManager.isEnabled()) {
            return;
        }

        ThemedMusicCatalog.load();

        shootSfx = AudioManager.loadSound(SFX_SHOOT);
        hitSfx = AudioManager.loadSound(SFX_HIT);
        transitionSfx = AudioManager.loadSound(SFX_TRANSITION);
        menuMusic = AudioManager.loadMusic(MUSIC_MENU);

        uiClickSfx.clear();
        for (String path : UI_CLICK_PATHS) {
            Sound s = AudioManager.loadSound(path);
            if (s != null) {
                uiClickSfx.add(s);
            }
        }

        logLoadSummary();
    }

    private static void logLoadSummary() {
        StringBuilder sb = new StringBuilder("Audio load: ");
        sb.append(status("shoot", shootSfx != null));
        sb.append(", hit=").append(status("hit", hitSfx != null));
        sb.append(", transition=").append(status("transition", transitionSfx != null));
        sb.append(", menu=").append(status("menu", menuMusic != null));
        sb.append(", uiClicks=").append(uiClickSfx.size()).append('/').append(UI_CLICK_PATHS.length);
        Logger.info("Audio", sb.toString());
    }

    private static String status(String name, boolean ok) {
        return name + (ok ? " OK" : " MISSING");
    }

    public static void playMenuMusic() {
        stopMusic();
        if (menuMusic != null) {
            menuMusic.playLoop(0.6f);
            currentMusic = menuMusic;
        }
    }

    public static void playThemedMusicForBlock(int blockIndex) {
        BackgroundCatalog.load();
        playRandomThemedMusic(BackgroundCatalog.getMusicThemesForBlock(blockIndex));
    }

    public static void playRandomThemedMusic(String... themeNames) {
        if (!AudioManager.isEnabled()) {
            return;
        }

        String path = ThemedMusicCatalog.randomTrack(themeNames);
        if (path == null) {
            return;
        }

        stopMusic();
        MusicTrack track = AudioManager.loadMusic(path);
        if (track != null) {
            track.playLoop(0.55f);
            currentMusic = track;
        }
    }

    public static void stopMusic() {
        AudioManager.stopMusic();
        if (menuMusic != null) {
            menuMusic.stop();
        }
        if (currentMusic != null) {
            currentMusic.stop();
        }
        currentMusic = null;
    }

    public static void pauseMusic() {
        if (currentMusic != null) {
            currentMusic.pause();
        }
    }

    public static void resumeMusic() {
        if (currentMusic != null) {
            currentMusic.resume();
        }
    }

    public static void playTransitionSfx() {
        if (transitionSfx != null) {
            transitionSfx.play(0.8f);
        }
    }

    public static void playShoot() {
        if (shootSfx != null) {
            shootSfx.play(0.35f);
        }
    }

    public static void playHit() {
        if (hitSfx != null) {
            hitSfx.play(0.5f);
        }
    }

    public static void playRandomUiClick() {
        if (uiClickSfx.isEmpty()) {
            return;
        }
        uiClickSfx.get(rng.nextInt(uiClickSfx.size())).play(0.65f);
    }

    /** @deprecated use {@link #playRandomUiClick()} */
    @Deprecated
    public static void playUiClick() {
        playRandomUiClick();
    }
}
