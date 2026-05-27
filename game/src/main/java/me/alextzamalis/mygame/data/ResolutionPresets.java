package me.alextzamalis.mygame.data;

/**
 * Fixed 4:3 window resolution presets aligned with the play area.
 */
public final class ResolutionPresets {

    public static final int COUNT = 4;

    private static final int[][] SIZES = {
            {800, 600},
            {1024, 768},
            {1280, 960},
            {1600, 1200}
    };

    private static final String[] LABELS = {
            "800x600",
            "1024x768",
            "1280x960",
            "1600x1200"
    };

    private ResolutionPresets() {
    }

    public static int clampIndex(int index) {
        if (index < 0) {
            return 0;
        }
        if (index >= COUNT) {
            return COUNT - 1;
        }
        return index;
    }

    public static int getWidth(int index) {
        return SIZES[clampIndex(index)][0];
    }

    public static int getHeight(int index) {
        return SIZES[clampIndex(index)][1];
    }

    public static String getLabel(int index) {
        return LABELS[clampIndex(index)];
    }

    public static int nextIndex(int index) {
        return (clampIndex(index) + 1) % COUNT;
    }
}
