import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class RecoveryManager {

    public static State recover(
            String transactionId,
            String logFile,
            ProtocolType protocolType
    ) {

        try {

            List<String> lines =
                    Files.readAllLines(Paths.get(logFile));

            for (String line : lines) {

                if (!line.startsWith(transactionId)) {
                    continue;
                }

                if (line.contains("COMMIT")) {
                    return State.COMMITTED;
                }

                if (line.contains("ABORT")) {
                    return State.ABORTED;
                }
            }

            return applyProtocolRule(protocolType);

        } catch (IOException e) {

            return applyProtocolRule(protocolType);
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

            case TWO_PC:
            default:
                return State.BLOCKED;
        }
    }
}