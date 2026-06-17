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

    public boolean receivePreCommit(String transactionId) {
        if (state == State.READ_ONLY) {
            log("ignora PRE_COMMIT porque ja encerrou como READ_ONLY.");
            return false;
        }

        if (state == State.TIMEOUT) {
            log("nao recebeu PRE_COMMIT porque estava indisponivel.");
            return false;
        }

        if (state == State.PRE_COMMITTED) {
            log("ja estava PRE_COMMITTED e reenviou ACK de PRE_COMMIT.");
            return true;
        }

        if (state == State.READY) {
            state = State.PRE_COMMITTED;
            persist(transactionId, "PRE_COMMIT");
            log("gravou PRE_COMMIT no log local e enviou ACK.");
            return true;
        }

        log("nao pode aplicar PRE_COMMIT porque seu estado atual e "
                + state.getDescription() + ".");
        return false;
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
            String commitMessage = protocolType == ProtocolType.THREE_PC ? "DO_COMMIT" : "COMMIT";
            state = State.COMMITTED;
            persist(transactionId, "COMMIT");
            if (ackRequired) {
                log("recebeu " + commitMessage
                        + ", gravou COMMIT no log, confirmou as alteracoes e enviou ACK.");
                return true;
            }

            log("recebeu " + commitMessage
                    + " e confirmou as alteracoes; ACK dispensado por "
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
        log("iniciando recuperacao local da transacao " + transactionId + ".");

        State recoveredState =
                RecoveryManager.recoverParticipant(
                        transactionId,
                        logFile,
                        protocolType
                );

        state = recoveredState;

        log("estado apos recuperacao local: " + ConsoleColors.state(recoveredState) + ".");
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
