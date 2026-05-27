package me.alextzamalis.mygame.data;

/**
 * Target FPS options for the settings menu.
 */
public final class FpsPresets {

    /** Sentinel value meaning uncapped (no V-Sync, no sleep). */
    public static final int MAX = 0;

    private static final int[] VALUES = {
            30, 60, 100, 120, 144, 165, 180, 240, 360, MAX
    };

    private static final String[] LABELS = {
            "30", "60", "100", "120", "144", "165", "180", "240", "360", "MAX"
    };

    private FpsPresets() {
    }

    public static int getCount() {
        return VALUES.length;
    }

    public static int getValue(int index) {
        return VALUES[clampIndex(index)];
    }

    public static String getLabel(int index) {
        return LABELS[clampIndex(index)];
    }

    public static int indexOfValue(int fpsValue) {
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i] == fpsValue) {
                return i;
            }
        }
        return 1;
    }

    public static int nextIndex(int index) {
        return (clampIndex(index) + 1) % VALUES.length;
    }

    public static int clampIndex(int index) {
        if (index < 0) {
            return 0;
        }
        if (index >= VALUES.length) {
            return VALUES.length - 1;
        }
        return index;
    }
}
