public class Participant {
    private final String name;
    private final ParticipantBehavior behavior;
    private State state;

    private ProtocolType protocolType;
    private final String logFile;

    public Participant(String name, ParticipantBehavior behavior, ProtocolType protocolType) {
        this.name = name;
        this.behavior = behavior;
        this.state = State.INITIAL;

        this.logFile = name.replace(" ", "_") + ".log";
        this.protocolType = protocolType;
    }

    public Vote prepare(String transactionId) {
        log("recebeu PREPARE da transacao " + transactionId + ".");
        state = State.VOTING;

        Vote vote = behavior.getVote();
        switch (vote) {
            case YES:
                state = State.READY;
                persist(transactionId, "READY");
                log("gravou estado READY no log local e respondeu YES.");
                break;
            case NO:
                state = State.ABORTED;
                persist(transactionId, "VOTE_ABORT");
                log("nao conseguiu preparar a transacao, respondeu NO e abortou localmente.");
                break;
            case READ_ONLY:
                state = State.READ_ONLY;
                log("nao possui alteracoes, respondeu READ_ONLY e encerrou sua participacao.");
                break;
            case TIMEOUT:
                state = State.TIMEOUT;
                log("nao respondeu dentro do tempo limite. Coordenador interpreta como TIMEOUT.");
                break;
            default:
                throw new IllegalStateException("Voto desconhecido: " + vote);
        }

        return vote;
    }

    public void receiveDecision(String transactionId, State decision) {
        if (state == State.READ_ONLY) {
            log("recebeu decisao " + decision + ", mas ja estava READ_ONLY e nao precisa aplicar nada.");
            return;
        }

        if (state == State.TIMEOUT) {
            log("nao recebeu a decisao " + decision + " porque estava indisponivel.");
            return;
        }

        if (decision == State.COMMITTED) {
            state = State.COMMITTED;
            persist(transactionId, "GLOBAL_COMMIT");
            log("recebeu COMMIT da transacao " + transactionId + " e confirmou as alteracoes.");
            return;
        }

        if (decision == State.ABORTED) {
            state = State.ABORTED;
            persist(transactionId, "GLOBAL_ABORT");
            log("recebeu ABORT da transacao " + transactionId + " e desfez/descartou alteracoes.");
            return;
        }

        log("recebeu decisao inesperada: " + decision + ".");
    }

    public void blockWaitingForCoordinator(String transactionId) {
        if (state == State.READY) {
            state = State.BLOCKED;
            log("votou YES na transacao " + transactionId
                    + " e ficou BLOQUEADO aguardando a decisao do coordenador.");
        } else if (state == State.READ_ONLY) {
            log("nao ficou bloqueado porque votou READ_ONLY.");
        } else {
            log("nao ficou bloqueado porque seu estado atual e " + state.getDescription() + ".");
        }
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }

    private void persist(String transactionId, String event) {
        TransactionLogger.write(
                logFile,
                transactionId + " " + event
        );
    }

    public void recover(String transactionId) {

        log("iniciando recuperacao da transacao "
                + transactionId + ".");

        State recoveredState =
                RecoveryManager.recover(
                        transactionId,
                        "Coordenador.log",
                        protocolType
                );

        state = recoveredState;

        log("estado apos recuperacao: "
                + recoveredState.getDescription());
    }

    private void log(String message) {
        System.out.println("[" + name + "] " + message);
    }
}
