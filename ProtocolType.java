public enum ProtocolType {
    TWO_PC("2PC", "2PC tradicional"),
    PRESUMED_ABORT("PA", "Presumed Abort"),
    PRESUMED_COMMIT("PC", "Presumed Commit"),
    THREE_PC("3PC", "Three-Phase Commit");

    private final String prefix;
    private final String description;

    ProtocolType(String prefix, String description) {
        this.prefix = prefix;
        this.description = description;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDescription() {
        return description;
    }
}
