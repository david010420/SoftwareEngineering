package repository;

import model.Role;
import model.UserAccount;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FileUserRepository implements UserRepository {
    private final Path path;

    public FileUserRepository(Path path) {
        this.path = path;
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public void save(UserAccount userAccount) {
        FileStore store = FileStore.load(path);
        store.users().removeIf(user -> user.getUsername().equalsIgnoreCase(userAccount.getUsername()));
        store.users().add(userAccount);
        FileStore.save(path, store);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return FileStore.load(path).users().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }

    @Override
    public List<UserAccount> findAll() {
        return FileStore.load(path).users().stream()
                .sorted(Comparator.comparing(UserAccount::getUsername))
                .toList();
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        return FileStore.load(path).users().stream()
                .filter(user -> user.getRole() == role)
                .sorted(Comparator.comparing(UserAccount::getUsername))
                .toList();
    }

    @Override
    public UserAccount delete(String username) {
        UserAccount user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("user not found: " + username));
        FileStore store = FileStore.load(path);
        store.users().removeIf(existing -> existing.getUsername().equalsIgnoreCase(username.trim()));
        FileStore.save(path, store);
        return user;
    }
}
