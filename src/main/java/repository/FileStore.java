package repository;

import model.Issue;
import model.Project;
import model.UserAccount;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class FileStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Issue> issues = new ArrayList<>();
    private final List<Project> projects = new ArrayList<>();
    private final List<UserAccount> users = new ArrayList<>();

    List<Issue> issues() {
        return issues;
    }

    List<Project> projects() {
        return projects;
    }

    List<UserAccount> users() {
        return users;
    }

    static synchronized FileStore load(Path path) {
        if (!Files.exists(path)) {
            return new FileStore();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
            Object value = input.readObject();
            if (value instanceof FileStore store) {
                return store;
            }
            return new FileStore();
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Failed to load store: " + path, exception);
        }
    }

    static synchronized void save(Path path, FileStore store) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(path))) {
                output.writeObject(store);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save store: " + path, exception);
        }
    }
}
