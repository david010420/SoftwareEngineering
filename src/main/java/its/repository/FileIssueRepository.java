package its.repository;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileIssueRepository implements IssueRepository {
    private final Path filePath;
    private IssueStore cached;

    public FileIssueRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public synchronized IssueStore load() {
        if (cached != null) {
            return cached;
        }
        if (!Files.exists(filePath)) {
            cached = new IssueStore();
            return cached;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(filePath))) {
            cached = (IssueStore) in.readObject();
            return cached;
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load store: " + filePath, e);
        }
    }

    @Override
    public synchronized void save(IssueStore store) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(filePath))) {
                out.writeObject(store);
            }
            cached = store;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save store: " + filePath, e);
        }
    }
}
