import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RecoveryManager {

    public static State recover(
            String transactionId,
            String logFile,
            ProtocolType protocolType
    ) {
        return recoverCoordinator(transactionId, logFile, protocolType);
    }

    public static State recoverCoordinator(
            String transactionId,
            String logFile,
            ProtocolType protocolType
    ) {
        List<String> events = readEvents(transactionId, logFile);

        if (events.contains("GLOBAL_COMMIT")) {
            return State.COMMITTED;
        }

        if (events.contains("GLOBAL_ABORT")) {
            return State.ABORTED;
        }

        if (protocolType == ProtocolType.THREE_PC) {
            if (events.contains("PRE_COMMIT")) {
                return State.COMMITTED;
            }

            return State.ABORTED;
        }

        if (events.contains("FORGOTTEN")) {
            return applyForgottenRule(protocolType);
        }

        if (protocolType == ProtocolType.PRESUMED_COMMIT && events.contains("COMMIT_INIT")) {
            return State.ABORTED;
        }

        return applyProtocolRule(protocolType);
    }

    public static State recoverParticipant(
            String transactionId,
            String logFile,
            ProtocolType protocolType
    ) {
        List<String> events = readEvents(transactionId, logFile);

        if (events.contains("COMMIT")) {
            return State.COMMITTED;
        }

        if (events.contains("ABORT") || events.contains("VOTE_ABORT")) {
            return State.ABORTED;
        }

        if (protocolType == ProtocolType.THREE_PC) {
            if (events.contains("PRE_COMMIT")) {
                return State.PRE_COMMITTED;
            }

            if (events.contains("READY")) {
                return State.ABORTED;
            }

            return State.ABORTED;
        }

        if (events.contains("READY")) {
            return applyProtocolRule(protocolType);
        }

        return applyProtocolRule(protocolType);
    }

    public static List<String> readEvents(String transactionId, String logFile) {
        List<String> events = new ArrayList<>();

        try {
            List<String> lines =
                    Files.readAllLines(Paths.get(logFile));

            for (String line : lines) {
                if (!line.startsWith(transactionId + " ")) {
                    continue;
                }

                events.add(line.substring(transactionId.length() + 1).trim());
            }
        } catch (IOException e) {
            return events;
        }

        return events;
    }

    private static State applyForgottenRule(ProtocolType protocolType) {
        switch (protocolType) {
            case PRESUMED_ABORT:
                return State.ABORTED;
            case PRESUMED_COMMIT:
                return State.COMMITTED;
            case THREE_PC:
                return State.ABORTED;
            case TWO_PC:
            default:
                return State.BLOCKED;
        }
    }

    private static State applyProtocolRule(
            ProtocolType protocolType
    ) {
        switch (protocolType) {
            case PRESUMED_ABORT:
                return State.ABORTED;
            case PRESUMED_COMMIT:
                return State.COMMITTED;
            case THREE_PC:
                return State.ABORTED;
            case TWO_PC:
            default:
                return State.BLOCKED;
        }
    }
}
