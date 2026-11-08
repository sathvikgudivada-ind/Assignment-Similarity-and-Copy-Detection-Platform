import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReaderService {
    public String readFile(String filePath) throws IOException {
        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + filePath);
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IOException("File is not a readable regular file: " + filePath);
        }

        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
