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
        List<Vote> votes = new ArrayList<>();

        persist(transactionId, "START");

        log("iniciando transacao " + transactionId + ".");
        state = State.VOTING;
        persist(transactionId, "VOTING");

        // Fase 1: o coordenador pergunta se cada participante pode confirmar.
        log("FASE 1 - VOTACAO: enviando PREPARE para os participantes.");
        for (Participant participant : participants) {
            Vote vote = participant.prepare(transactionId);
            votes.add(vote);
            log("recebeu voto de " + participant.getName() + ": " + vote.getDescription() + ".");
        }

        if (failBeforeDecision) {
            state = State.FAILED;
            log("FALHA SIMULADA: coordenador caiu apos a votacao e antes da fase de decisao.");
            log("Como nao houve decisao global, participantes preparados podem ficar bloqueados.");
            for (Participant participant : participants) {
                participant.blockWaitingForCoordinator(transactionId);
            }
            printFinalStates();
            return state;
        }

        State decision = decide(votes);
        state = decision;

        // Registrar a decisão no log antes de enviá-la
        if (decision == State.COMMITTED) {
            persist(transactionId, "GLOBAL_COMMIT");
        } else if (decision == State.ABORTED) {
            persist(transactionId, "GLOBAL_ABORT");
        }

        // Fase 2: depois de decidir, o coordenador avisa todos os participantes.
        log("FASE 2 - DECISAO: decisao global = " + decision + " (" + decision.getDescription() + ").");
        for (Participant participant : participants) {
            participant.receiveDecision(transactionId, decision);
        }

        printFinalStates();
        return state;
    }

    public State recoverTransaction(String transactionId) {

        log("iniciando recuperacao da transacao "
                + transactionId);

        State recoveredState =
                RecoveryManager.recover(
                        transactionId,
                        logFile,
                        protocolType
                );

        log("resultado da recuperacao: "
                + recoveredState.getDescription());

        return recoveredState;
    }

    private State decide(List<Vote> votes) {
        for (Vote vote : votes) {
            if (vote == Vote.NO || vote == Vote.TIMEOUT) {
                log("existe voto " + vote + ". Pela regra do 2PC, a decisao sera ABORT.");
                return State.ABORTED;
            }
        }

        log("todos os participantes ativos votaram YES ou READ_ONLY. A decisao sera COMMIT.");
        return State.COMMITTED;
    }

    private void printFinalStates() {
        log("estado final do coordenador: " + state.getDescription() + ".");
        for (Participant participant : participants) {
            log("estado final de " + participant.getName() + ": "
                    + participant.getState().getDescription() + ".");
        }
    }

    private void persist(String transactionId, String event) {
        TransactionLogger.write(
                logFile,
                transactionId + " " + event
        );
    }

    private void log(String message) {
        System.out.println("[" + name + "] " + message);
    }
}
