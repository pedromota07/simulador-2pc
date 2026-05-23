public enum ParticipantBehavior {
    VOTE_YES(Vote.YES),
    VOTE_NO(Vote.NO),
    READ_ONLY(Vote.READ_ONLY),
    TIMEOUT(Vote.TIMEOUT);

    private final Vote vote;

    ParticipantBehavior(Vote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }
}
