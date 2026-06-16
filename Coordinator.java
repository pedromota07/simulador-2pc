import java.util.ArrayList;
import java.util.List;

public class Coordinator {
    private final String name;
    private final List<Participant> participants;
    private State state;

    private final String logFile;
    private final ProtocolType protocolType;

    public Coordinator(String name, List<Participant> participants, ProtocolType protocolType) {
        this.name = name;
        this.participants = participants;
        this.state = State.INITIAL;

        this.protocolType = protocolType;
        this.logFile = name.replace(" ", "_") + ".log";
    }

    public State executeTransaction(String transactionId, boolean failBeforeDecision) {
        CoordinatorFailurePoint failurePoint = failBeforeDecision
                ? CoordinatorFailurePoint.BEFORE_GLOBAL_COMMIT
                : CoordinatorFailurePoint.NONE;

        return executeTransaction(transactionId, failurePoint);
    }

    public State executeTransaction(String transactionId, CoordinatorFailurePoint failurePoint) {
        List<Vote> votes = new ArrayList<>();
        List<Participant> preparedParticipants = new ArrayList<>();

        log("iniciando transacao " + transactionId + " usando " + protocolType.getDescription() + ".");
        initializeTransactionLog(transactionId);

        state = State.VOTING;

        log("FASE 1 - VOTACAO: enviando PREPARE para os participantes.");
        for (Participant participant : participants) {
            Vote vote = participant.prepare(transactionId);
            votes.add(vote);

            if (vote == Vote.YES) {
                preparedParticipants.add(participant);
            }

            log("voto de " + participant.getName() + ": " + vote.getDescription() + ".");
        }

        if (failurePoint == CoordinatorFailurePoint.BEFORE_GLOBAL_COMMIT) {
            state = State.FAILED;
            log("FALHA SIMULADA: coordenador caiu apos a votacao e antes de GLOBAL_COMMIT.");
            blockPreparedParticipants(transactionId, preparedParticipants);
            printFinalStates();
            return state;
        }

        State decision = decide(votes);
        state = decision;

        if (decision == State.COMMITTED) {
            handleCommit(transactionId, preparedParticipants, failurePoint);
        } else {
            handleAbort(transactionId, preparedParticipants);
        }

        printFinalStates();
        return state;
    }

    public State recoverTransaction(String transactionId) {
        log("iniciando recuperacao da transacao " + transactionId + ".");

        List<String> events = RecoveryManager.readEvents(transactionId, logFile);
        explainRecovery(events);

        State recoveredState = RecoveryManager.recover(transactionId, logFile, protocolType);
        log("resultado da recuperacao: " + ConsoleColors.state(recoveredState) + ".");

        if (recoveredState == State.COMMITTED) {
            List<Participant> preparedParticipants = getBlockedOrReadyParticipants();
            if (preparedParticipants.isEmpty()) {
                log("nao ha participantes preparados aguardando recuperacao.");
                return recoveredState;
            }

            boolean ackRequired = requiresAck(State.COMMITTED);
            log("reenviando COMMIT aos participantes preparados.");
            sendDecisionToPrepared(transactionId, State.COMMITTED, preparedParticipants, ackRequired);
            if (ackRequired) {
                log("ACKs de COMMIT recebidos; coordenador pode esquecer a transacao.");
            }
            return recoveredState;
        }

        if (recoveredState == State.ABORTED) {
            List<Participant> preparedParticipants = getBlockedOrReadyParticipants();
            if (preparedParticipants.isEmpty()) {
                log("nao ha participantes preparados aguardando recuperacao.");
                return recoveredState;
            }

            boolean ackRequired = requiresAck(State.ABORTED);
            log("respondendo ABORT aos participantes em recuperacao.");
            sendDecisionToPrepared(transactionId, State.ABORTED, preparedParticipants, ackRequired);
            if (ackRequired) {
                log("ACKs de ABORT recebidos; coordenador pode esquecer a transacao.");
            }
            return recoveredState;
        }

        log("sem decisao global no log; participantes preparados continuam bloqueados.");
        return recoveredState;
    }

    private void initializeTransactionLog(String transactionId) {
        if (protocolType == ProtocolType.PRESUMED_COMMIT) {
            persist(transactionId, "COMMIT_INIT");
            log("gravou COMMIT_INIT no log antes de enviar PREPARE.");
            return;
        }

        if (protocolType == ProtocolType.PRESUMED_ABORT) {
            log("Presumed Abort nao grava registro inicial antes do PREPARE.");
            return;
        }

        persist(transactionId, "START");
        log("gravou START no log antes de enviar PREPARE.");
    }

    private State decide(List<Vote> votes) {
        for (Vote vote : votes) {
            if (vote == Vote.NO || vote == Vote.TIMEOUT) {
                log("existe voto " + vote + ". A decisao sera ABORT.");
                return State.ABORTED;
            }
        }

        log("todos votaram YES ou READ_ONLY. A decisao sera COMMIT.");
        return State.COMMITTED;
    }

    private void handleCommit(
            String transactionId,
            List<Participant> preparedParticipants,
            CoordinatorFailurePoint failurePoint
    ) {
        persist(transactionId, "GLOBAL_COMMIT");
        log("gravou GLOBAL_COMMIT no log antes de enviar COMMIT.");

        if (failurePoint == CoordinatorFailurePoint.AFTER_GLOBAL_COMMIT) {
            state = State.FAILED;
            log("FALHA SIMULADA: coordenador caiu apos GLOBAL_COMMIT e antes de enviar COMMIT.");
            blockPreparedParticipants(transactionId, preparedParticipants);
            return;
        }

        boolean ackRequired = requiresAck(State.COMMITTED);
        if (protocolType == ProtocolType.PRESUMED_COMMIT) {
            log("Presumed Commit dispensa ACKs no caso de COMMIT.");
        }

        log("FASE 2 - DECISAO: enviando COMMIT aos participantes preparados.");
        int acknowledgements = sendDecisionToPrepared(transactionId, State.COMMITTED, preparedParticipants, ackRequired);

        if (ackRequired) {
            log("recebeu " + acknowledgements + " ACK(s) de COMMIT e pode esquecer a transacao.");
        } else {
            log("COMMIT enviado; coordenador pode esquecer a transacao.");
        }

        persist(transactionId, "FORGOTTEN");
    }

    private void handleAbort(String transactionId, List<Participant> preparedParticipants) {
        boolean ackRequired = requiresAck(State.ABORTED);

        if (protocolType == ProtocolType.PRESUMED_ABORT) {
            log("Presumed Abort nao grava GLOBAL_ABORT e nao espera ACK de ABORT.");
        } else if (protocolType == ProtocolType.PRESUMED_COMMIT) {
            log("Presumed Commit precisa receber ACKs de ABORT dos participantes preparados.");
        } else {
            persist(transactionId, "GLOBAL_ABORT");
            log("gravou GLOBAL_ABORT no log antes de enviar ABORT.");
        }

        log("FASE 2 - DECISAO: enviando ABORT aos participantes preparados.");
        int acknowledgements = sendDecisionToPrepared(transactionId, State.ABORTED, preparedParticipants, ackRequired);

        if (ackRequired) {
            log("recebeu " + acknowledgements + " ACK(s) de ABORT e pode esquecer a transacao.");
        } else {
            log("esqueceu a transacao; futuras consultas serao respondidas como ABORT por presuncao.");
        }

        persist(transactionId, "FORGOTTEN");
    }

    private boolean requiresAck(State decision) {
        if (protocolType == ProtocolType.PRESUMED_ABORT) {
            return decision == State.COMMITTED;
        }

        if (protocolType == ProtocolType.PRESUMED_COMMIT) {
            return decision == State.ABORTED;
        }

        return true;
    }

    private int sendDecisionToPrepared(
            String transactionId,
            State decision,
            List<Participant> preparedParticipants,
            boolean ackRequired
    ) {
        int acknowledgements = 0;

        if (preparedParticipants.isEmpty()) {
            log("nao ha participantes preparados para receber " + decision + ".");
            return acknowledgements;
        }

        for (Participant participant : preparedParticipants) {
            boolean acknowledged = participant.receiveDecision(transactionId, decision, ackRequired);
            if (acknowledged) {
                acknowledgements++;
            }
        }

        return acknowledgements;
    }

    private void blockPreparedParticipants(String transactionId, List<Participant> preparedParticipants) {
        for (Participant participant : preparedParticipants) {
            participant.blockWaitingForCoordinator(transactionId);
        }
    }

    private List<Participant> getBlockedOrReadyParticipants() {
        List<Participant> preparedParticipants = new ArrayList<>();

        for (Participant participant : participants) {
            State participantState = participant.getState();
            if (participantState == State.READY || participantState == State.BLOCKED) {
                preparedParticipants.add(participant);
            }
        }

        return preparedParticipants;
    }

    private void explainRecovery(List<String> events) {
        boolean hasGlobalCommit = events.contains("GLOBAL_COMMIT");
        boolean hasCommitInit = events.contains("COMMIT_INIT");

        if (hasGlobalCommit) {
            log("encontrou GLOBAL_COMMIT no log.");
            return;
        }

        if (protocolType == ProtocolType.PRESUMED_ABORT) {
            log("nao encontrou GLOBAL_COMMIT no log.");
            log("Presumed Abort: ausencia de decisao de commit implica ABORT.");
            return;
        }

        if (protocolType == ProtocolType.PRESUMED_COMMIT && hasCommitInit) {
            log("recuperou COMMIT_INIT, mas nao encontrou GLOBAL_COMMIT.");
            log("a transacao estava pendente; nao e correto presumir COMMIT nesse ponto.");
            log("politica de recuperacao: ABORT seguro.");
            return;
        }

        if (protocolType == ProtocolType.PRESUMED_COMMIT) {
            log("nao encontrou informacao ativa; Presumed Commit presume COMMIT para transacao esquecida.");
        }
    }

    private void printFinalStates() {
        log("estado final do coordenador: " + ConsoleColors.state(state) + ".");
        for (Participant participant : participants) {
            log("estado final de " + participant.getName() + ": "
                    + ConsoleColors.state(participant.getState()) + ".");
        }
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
