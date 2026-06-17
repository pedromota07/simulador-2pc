public class ConsoleColors {
    private static final boolean ENABLED = System.getenv("NO_COLOR") == null;

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";

    public static String red(String text) {
        return color(text, RED);
    }

    public static String green(String text) {
        return color(text, GREEN);
    }

    public static String yellow(String text) {
        return color(text, YELLOW);
    }

    public static String cyan(String text) {
        return color(text, CYAN);
    }

    public static String actor(String name) {
        if (name.startsWith("Coordenador")) {
            return color("[" + name + "]", BLUE);
        }

        if (name.startsWith("Participante")) {
            return color("[" + name + "]", MAGENTA);
        }

        return "[" + name + "]";
    }

    public static String state(State state) {
        switch (state) {
            case COMMITTED:
                return green(state.getDescription());
            case ABORTED:
            case FAILED:
                return red(state.getDescription());
            case BLOCKED:
            case TIMEOUT:
            case PRE_COMMITTING:
            case PRE_COMMITTED:
                return yellow(state.getDescription());
            case READ_ONLY:
                return cyan(state.getDescription());
            default:
                return state.getDescription();
        }
    }
    private static String color(String text, String color) {
        if (!ENABLED) {
            return text;
        }

        return color + text + RESET;
    }
}
