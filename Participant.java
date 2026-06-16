public class Participant {
    private final String name;
    private final ParticipantBehavior behavior;
    private State state;

    private final ProtocolType protocolType;
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
                log("gravou READY no log local e respondeu YES.");
                break;
            case NO:
                state = State.ABORTED;
                persist(transactionId, "VOTE_ABORT");
                log("respondeu NO e abortou localmente.");
                break;
            case READ_ONLY:
                state = State.READ_ONLY;
                log("respondeu READ_ONLY e encerrou sua participacao; nao entrara na fase de decisao.");
                break;
            case TIMEOUT:
                state = State.TIMEOUT;
                log("nao respondeu dentro do tempo limite.");
                break;
            default:
                throw new IllegalStateException("Voto desconhecido: " + vote);
        }

        return vote;
    }

    public void receiveDecision(String transactionId, State decision) {
        receiveDecision(transactionId, decision, false);
    }

    public boolean receiveDecision(String transactionId, State decision, boolean ackRequired) {
        if (state == State.READ_ONLY) {
            log("ignora " + decision + " porque ja encerrou como READ_ONLY.");
            return false;
        }

        if (state == State.TIMEOUT) {
            log("nao recebeu " + decision + " porque estava indisponivel.");
            return false;
        }

        if (decision == State.COMMITTED) {
            state = State.COMMITTED;
            persist(transactionId, "COMMIT");
            if (ackRequired) {
                log("gravou COMMIT no log, confirmou as alteracoes e enviou ACK.");
                return true;
            }

            log("confirmou as alteracoes; ACK de COMMIT dispensado por "
                    + protocolType.getDescription() + ".");
            return false;
        }

        if (decision == State.ABORTED) {
            state = State.ABORTED;
            if (ackRequired) {
                persist(transactionId, "ABORT");
                log("gravou ABORT no log, descartou as alteracoes e enviou ACK.");
                return true;
            }

            log("recebeu ABORT, descartou as alteracoes e esqueceu a transacao sem ACK.");
            return false;
        }

        log("recebeu decisao inesperada: " + decision + ".");
        return false;
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

    public void recover(String transactionId) {
        log("consultando o coordenador para recuperar a transacao " + transactionId + ".");

        State recoveredState =
                RecoveryManager.recover(
                        transactionId,
                        "Coordenador.log",
                        protocolType
                );

        state = recoveredState;

        log("estado apos consulta: " + ConsoleColors.state(recoveredState) + ".");
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

    private void log(String message) {
        System.out.println(ConsoleColors.actor(name) + " " + message);
    }
}
