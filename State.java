public enum State {
    INITIAL("Inicial"),
    VOTING("Em votacao"),
    READY("Preparado"),
    COMMITTED("Commit realizado"),
    ABORTED("Abort realizado"),
    READ_ONLY("Somente leitura"),
    TIMEOUT("Timeout"),
    BLOCKED("Bloqueado"),
    FAILED("Falhou");

    private final String description;

    State(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
