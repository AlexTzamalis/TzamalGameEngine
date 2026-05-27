package me.alextzamalis.engine.core;

/**
 * Simple static logging utility for the engine and game code.
 *
 * <p>Messages are formatted as {@code [LEVEL] [tag] message} and
 * printed to stdout (INFO, DEBUG) or stderr (WARN, ERROR).
 * Set the minimum level with {@link #setLevel(Level)} to control
 * verbosity at runtime.</p>
 *
 * @author Alexandros Tzamalis
 */
public final class Logger {

    public enum Level {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3),
        OFF(4);

        final int rank;

        Level(int rank) {
            this.rank = rank;
        }
    }

    private static Level minLevel = Level.DEBUG;

    private Logger() {
    }

    /** Sets the minimum log level. Messages below this level are discarded. */
    public static void setLevel(Level level) {
        minLevel = level;
    }

    /** @return the current minimum log level. */
    public static Level getLevel() {
        return minLevel;
    }

    public static void debug(String tag, String message) {
        log(Level.DEBUG, tag, message);
    }

    public static void info(String tag, String message) {
        log(Level.INFO, tag, message);
    }

    public static void warn(String tag, String message) {
        log(Level.WARN, tag, message);
    }

    public static void error(String tag, String message) {
        log(Level.ERROR, tag, message);
    }

    public static void error(String tag, String message, Throwable t) {
        if (Level.ERROR.rank >= minLevel.rank) {
            System.err.println("[ERROR] [" + tag + "] " + message);
            t.printStackTrace(System.err);
        }
    }

    private static void log(Level level, String tag, String message) {
        if (level.rank < minLevel.rank) {
            return;
        }
        String line = "[" + level.name() + "] [" + tag + "] " + message;
        if (level.rank >= Level.WARN.rank) {
            System.err.println(line);
        } else {
            System.out.println(line);
        }
    }
}
