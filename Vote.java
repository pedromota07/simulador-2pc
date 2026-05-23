public enum Vote {
    YES("YES - participante preparado para confirmar"),
    NO("NO - participante recusou a transacao"),
    READ_ONLY("READ_ONLY - participante nao alterou dados"),
    TIMEOUT("TIMEOUT - participante nao respondeu");

    private final String description;

    Vote(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
