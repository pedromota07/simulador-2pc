import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        runScenario("Cenario 1 - Commit com todos YES", "T1",
                ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES, false);

        runScenario("Cenario 2 - Abort com participante NO", "T2",
                ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_NO, ParticipantBehavior.VOTE_YES, false);

        runScenario("Cenario 3 - Falha/timeout de participante", "T3",
                ParticipantBehavior.VOTE_YES, ParticipantBehavior.TIMEOUT, ParticipantBehavior.VOTE_YES, false);

        runScenario("Cenario 4 - Participante READ_ONLY", "T4",
                ParticipantBehavior.VOTE_YES, ParticipantBehavior.READ_ONLY, ParticipantBehavior.VOTE_YES, false);

        runScenario("Cenario 5 - Falha do coordenador causando bloqueio", "T5",
                ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES, true);
    }

    private static void runScenario(
            String title,
            String transactionId,
            ParticipantBehavior behavior1,
            ParticipantBehavior behavior2,
            ParticipantBehavior behavior3,
            boolean coordinatorFailsBeforeDecision
    ) {
        System.out.println();
        System.out.println("============================================================");
        System.out.println(title);
        System.out.println("============================================================");

        List<Participant> participants = Arrays.asList(
                new Participant("Participante 1", behavior1),
                new Participant("Participante 2", behavior2),
                new Participant("Participante 3", behavior3)
        );

        Coordinator coordinator = new Coordinator("Coordenador", participants);
        State result = coordinator.executeTransaction(transactionId, coordinatorFailsBeforeDecision);

        System.out.println("[Main] Resultado do cenario: " + result.getDescription() + ".");
    }
}
