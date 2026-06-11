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

        System.out.println();
        System.out.println("============================================================");
        System.out.println("COMPARACAO DOS PROTOCOLOS");
        System.out.println("============================================================");

        Participant p1 =
                new Participant(
                        "Participante 2PC",
                        ParticipantBehavior.VOTE_YES,
                        ProtocolType.TWO_PC
                );

        p1.recover("T5");

        Participant p2 =
                new Participant(
                        "Participante PA",
                        ParticipantBehavior.VOTE_YES,
                        ProtocolType.PRESUMED_ABORT
                );

        p2.recover("T5");

        Participant p3 =
                new Participant(
                        "Participante PC",
                        ParticipantBehavior.VOTE_YES,
                        ProtocolType.PRESUMED_COMMIT
                );

        p3.recover("T5");

        System.out.println();
        System.out.println("============================================================");
        System.out.println("RECUPERACAO DO COORDENADOR");
        System.out.println("============================================================");
        Coordinator coordinator =
                new Coordinator(
                        "Coordenador",
                        List.of(),
                        ProtocolType.TWO_PC
                );

        coordinator.recoverTransaction("T1");  //testar com t5 e t1 --- caio luiz
    }

    private static void testRecovery(
            String transactionId,
            ProtocolType protocolType
    ) {

        State recoveredState =
                RecoveryManager.recover(
                        transactionId,
                        "Coordenador.log",
                        protocolType
                );

        System.out.println(
                "[Recovery] Transacao "
                        + transactionId
                        + " usando "
                        + protocolType
                        + " => "
                        + recoveredState.getDescription()
        );
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
                new Participant("Participante 1", behavior1, ProtocolType.TWO_PC),
                new Participant("Participante 2", behavior2, ProtocolType.TWO_PC),
                new Participant("Participante 3", behavior3, ProtocolType.TWO_PC)
        );

        Coordinator coordinator = new Coordinator("Coordenador", participants, ProtocolType.TWO_PC);
        State result = coordinator.executeTransaction(transactionId, coordinatorFailsBeforeDecision);

        System.out.println("[Main] Resultado do cenario: " + result.getDescription() + ".");
    }
}
