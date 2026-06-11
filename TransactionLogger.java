import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TransactionLogger {

    public static void write(String fileName, String message) {
        try (PrintWriter writer =
                     new PrintWriter(new FileWriter(fileName, true))) {

            writer.println(message);

        } catch (IOException e) {
            System.err.println("Erro ao gravar log: " + e.getMessage());
        }
    }
}