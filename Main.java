import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String MENU_LINE = "============================================================";
    private static final String COMPARISON_LINE = "############################################################";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            runInteractiveMenu(scanner);
        }
    }

    private static void runInteractiveMenu(Scanner scanner) {
        while (true) {
            int option = chooseProtocol(scanner);

            if (option == 0) {
                System.out.println("Encerrando simulador.");
                return;
            }

            if (option == 4) {
                runAllProtocolsScenarioMenu(scanner);
                continue;
            }

            ProtocolType protocolType = getProtocolByOption(option);
            if (protocolType != null) {
                runScenarioMenu(scanner, protocolType);
            }
        }
    }

    private static int chooseProtocol(Scanner scanner) {
        System.out.println();
        System.out.println(MENU_LINE);
        System.out.println("SIMULADOR 2PC - ESCOLHA O PROTOCOLO");
        System.out.println(MENU_LINE);
        System.out.println("1 - " + ProtocolType.TWO_PC.getDescription());
        System.out.println("2 - " + ProtocolType.PRESUMED_ABORT.getDescription());
        System.out.println("3 - " + ProtocolType.PRESUMED_COMMIT.getDescription());
        System.out.println("4 - Rodar todos os protocolos");
        System.out.println("0 - Sair");

        return readOption(scanner, 0, 4);
    }

    private static void runScenarioMenu(Scanner scanner, ProtocolType protocolType) {
        while (true) {
            System.out.println();
            System.out.println(MENU_LINE);
            System.out.println("PROTOCOLO SELECIONADO: " + protocolType.getDescription());
            System.out.println(MENU_LINE);
            printScenarioOptions("Rodar todos os cenarios deste protocolo");

            int option = readOption(scanner, 0, 8);

            if (option == 0) {
                return;
            }

            clearPreviousLogs();

            if (option == 8) {
                runProtocolScenarios(protocolType);
            } else {
                runSelectedScenario(protocolType, option);
            }
        }
    }

    private static void runAllProtocolsScenarioMenu(Scanner scanner) {
        while (true) {
            System.out.println();
            System.out.println(MENU_LINE);
            System.out.println("MODO COMPARACAO: TODOS OS PROTOCOLOS");
            System.out.println(MENU_LINE);
            printScenarioOptions("Rodar todos os cenarios de todos os protocolos");

            int option = readOption(scanner, 0, 8);

            if (option == 0) {
                return;
            }

            clearPreviousLogs();

            if (option == 8) {
                runAllProtocols();
            } else {
                runScenarioForAllProtocols(option);
            }
        }
    }

    private static void printScenarioOptions(String runAllLabel) {
        System.out.println("1 - Commit com todos YES");
        System.out.println("2 - Abort com participante NO");
        System.out.println("3 - Abort por TIMEOUT");
        System.out.println("4 - Commit com participante READ_ONLY");
        System.out.println("5 - Falha antes de GLOBAL_COMMIT");
        System.out.println("6 - Falha apos GLOBAL_COMMIT");
        System.out.println("7 - Participante preparado consulta o coordenador");
        System.out.println("8 - " + runAllLabel);
        System.out.println("0 - Voltar");
    }

    private static int readOption(Scanner scanner, int min, int max) {
        while (true) {
            System.out.print("Opcao: ");

            if (!scanner.hasNextLine()) {
                System.out.println();
                return 0;
            }

            String input = scanner.nextLine().trim();

            try {
                int option = Integer.parseInt(input);
                if (option >= min && option <= max) {
                    return option;
                }
            } catch (NumberFormatException e) {
                // Repete a leitura com mensagem amigavel abaixo.
            }

            System.out.println("Opcao invalida. Escolha um numero entre " + min + " e " + max + ".");
        }
    }

    private static ProtocolType getProtocolByOption(int option) {
        switch (option) {
            case 1:
                return ProtocolType.TWO_PC;
            case 2:
                return ProtocolType.PRESUMED_ABORT;
            case 3:
                return ProtocolType.PRESUMED_COMMIT;
            default:
                return null;
        }
    }

    private static void runAllProtocols() {
        for (ProtocolType protocolType : ProtocolType.values()) {
            runProtocolScenarios(protocolType);
        }
    }

    private static void runScenarioForAllProtocols(int scenario) {
        System.out.println();
        System.out.println(COMPARISON_LINE);
        System.out.println("COMPARACAO ENTRE PROTOCOLOS");
        System.out.println(getScenarioTitle(scenario));
        System.out.println(COMPARISON_LINE);

        for (ProtocolType protocolType : ProtocolType.values()) {
            runSelectedScenario(protocolType, scenario);
        }
    }

    private static void runProtocolScenarios(ProtocolType protocolType) {
        System.out.println();
        System.out.println(COMPARISON_LINE);
        System.out.println("PROTOCOLO: " + protocolType.getDescription());
        System.out.println(COMPARISON_LINE);

        for (int scenario = 1; scenario <= 7; scenario++) {
            runSelectedScenario(protocolType, scenario);
        }
    }

    private static void runSelectedScenario(ProtocolType protocolType, int scenario) {
        switch (scenario) {
            case 1:
                runScenario(getScenarioTitle(scenario), protocolType.getPrefix() + "_T1", protocolType,
                        ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES,
                        CoordinatorFailurePoint.NONE);
                break;
            case 2:
                runScenario(getScenarioTitle(scenario), protocolType.getPrefix() + "_T2", protocolType,
                        ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_NO, ParticipantBehavior.VOTE_YES,
                        CoordinatorFailurePoint.NONE);
                break;
            case 3:
                runScenario(getScenarioTitle(scenario), protocolType.getPrefix() + "_T3", protocolType,
                        ParticipantBehavior.VOTE_YES, ParticipantBehavior.TIMEOUT, ParticipantBehavior.VOTE_YES,
                        CoordinatorFailurePoint.NONE);
                break;
            case 4:
                runScenario(getScenarioTitle(scenario), protocolType.getPrefix() + "_T4",
                        protocolType, ParticipantBehavior.VOTE_YES, ParticipantBehavior.READ_ONLY,
                        ParticipantBehavior.VOTE_YES, CoordinatorFailurePoint.NONE);
                break;
            case 5:
                runScenario(getScenarioTitle(scenario), protocolType.getPrefix() + "_T5",
                        protocolType, ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES,
                        ParticipantBehavior.VOTE_YES, CoordinatorFailurePoint.BEFORE_GLOBAL_COMMIT);
                break;
            case 6:
                runScenario(getScenarioTitle(scenario), protocolType.getPrefix() + "_T6",
                        protocolType, ParticipantBehavior.VOTE_YES, ParticipantBehavior.VOTE_YES,
                        ParticipantBehavior.VOTE_YES, CoordinatorFailurePoint.AFTER_GLOBAL_COMMIT);
                break;
            case 7:
                runParticipantRecoveryScenario(protocolType);
                break;
            default:
                throw new IllegalArgumentException("Cenario desconhecido: " + scenario);
        }
    }

    private static String getScenarioTitle(int scenario) {
        switch (scenario) {
            case 1:
                return "Cenario 1 - Commit com todos YES";
            case 2:
                return "Cenario 2 - Abort com participante NO";
            case 3:
                return "Cenario 3 - Abort por TIMEOUT";
            case 4:
                return "Cenario 4 - Commit com participante READ_ONLY";
            case 5:
                return "Cenario 5 - Falha antes de GLOBAL_COMMIT";
            case 6:
                return "Cenario 6 - Falha apos GLOBAL_COMMIT";
            case 7:
                return "Cenario 7 - Participante preparado consulta o coordenador";
            default:
                throw new IllegalArgumentException("Cenario desconhecido: " + scenario);
        }
    }

    private static void runScenario(
            String title,
            String transactionId,
            ProtocolType protocolType,
            ParticipantBehavior behavior1,
            ParticipantBehavior behavior2,
            ParticipantBehavior behavior3,
            CoordinatorFailurePoint failurePoint
    ) {
        System.out.println();
        System.out.println(MENU_LINE);
        System.out.println("PROTOCOLO : " + protocolType.getDescription());
        System.out.println("CENARIO   : " + title);
        System.out.println("TRANSACAO : " + transactionId);
        System.out.println(MENU_LINE);

        List<Participant> participants = createParticipants(protocolType, behavior1, behavior2, behavior3);
        Coordinator coordinator = new Coordinator("Coordenador", participants, protocolType);

        State result = coordinator.executeTransaction(transactionId, failurePoint);
        System.out.println("[Main] Resultado do cenario: " + ConsoleColors.state(result) + ".");

        if (failurePoint != CoordinatorFailurePoint.NONE) {
            System.out.println("[Main] Recuperacao do coordenador apos falha:");
            State recoveredState = coordinator.recoverTransaction(transactionId);
            System.out.println("[Main] Resultado da recuperacao: "
                    + ConsoleColors.state(recoveredState) + ".");
        }
    }

    private static void runParticipantRecoveryScenario(ProtocolType protocolType) {
        System.out.println();
        System.out.println(MENU_LINE);
        System.out.println("PROTOCOLO : " + protocolType.getDescription());
        System.out.println("CENARIO   : " + getScenarioTitle(7));
        System.out.println("TRANSACAO : " + protocolType.getPrefix() + "_T7");
        System.out.println(MENU_LINE);

        if (protocolType == ProtocolType.PRESUMED_COMMIT) {
            runPendingPresumedCommitRecovery();
            runForgottenPresumedCommitRecovery();
            return;
        }

        String transactionId = protocolType.getPrefix() + "_T7";
        List<Participant> participants = createParticipants(
                protocolType,
                ParticipantBehavior.VOTE_YES,
                ParticipantBehavior.VOTE_YES,
                ParticipantBehavior.VOTE_YES
        );
        Coordinator coordinator = new Coordinator("Coordenador", participants, protocolType);

        coordinator.executeTransaction(transactionId, CoordinatorFailurePoint.BEFORE_GLOBAL_COMMIT);
        System.out.println("[Main] Participantes preparados consultam o coordenador.");

        State recoveredState = coordinator.recoverTransaction(transactionId);
        System.out.println("[Main] Resposta do coordenador: "
                + ConsoleColors.state(recoveredState) + ".");

        for (Participant participant : participants) {
            participant.recover(transactionId);
        }
    }

    private static void runPendingPresumedCommitRecovery() {
        String transactionId = "PC_T7_PENDING";
        List<Participant> participants = createParticipants(
                ProtocolType.PRESUMED_COMMIT,
                ParticipantBehavior.VOTE_YES,
                ParticipantBehavior.VOTE_YES,
                ParticipantBehavior.VOTE_YES
        );
        Coordinator coordinator = new Coordinator("Coordenador", participants, ProtocolType.PRESUMED_COMMIT);

        coordinator.executeTransaction(transactionId, CoordinatorFailurePoint.BEFORE_GLOBAL_COMMIT);
        System.out.println("[Main] Consulta com COMMIT_INIT pendente: nao deve presumir COMMIT.");

        State recoveredState = coordinator.recoverTransaction(transactionId);
        System.out.println("[Main] Resposta do coordenador: "
                + ConsoleColors.state(recoveredState) + ".");
    }

    private static void runForgottenPresumedCommitRecovery() {
        String transactionId = "PC_T7_FORGOTTEN";
        Coordinator coordinator = new Coordinator("Coordenador", List.of(), ProtocolType.PRESUMED_COMMIT);

        System.out.println("[Main] Consulta sobre transacao esquecida sem log ativo.");
        State recoveredState = coordinator.recoverTransaction(transactionId);
        System.out.println("[Main] Resposta do coordenador: "
                + ConsoleColors.state(recoveredState) + ".");
    }

    private static List<Participant> createParticipants(
            ProtocolType protocolType,
            ParticipantBehavior behavior1,
            ParticipantBehavior behavior2,
            ParticipantBehavior behavior3
    ) {
        return Arrays.asList(
                new Participant("Participante 1", behavior1, protocolType),
                new Participant("Participante 2", behavior2, protocolType),
                new Participant("Participante 3", behavior3, protocolType)
        );
    }

    private static void clearPreviousLogs() {
        File currentDirectory = new File(".");
        File[] logFiles = currentDirectory.listFiles((directory, name) -> name.endsWith(".log"));

        if (logFiles == null) {
            return;
        }

        for (File logFile : logFiles) {
            if (!logFile.delete()) {
                System.err.println("[Main] Nao foi possivel apagar log antigo: " + logFile.getName());
            }
        }
    }
}
