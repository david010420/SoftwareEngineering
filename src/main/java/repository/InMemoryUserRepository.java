package repository;

import model.Role;
import model.UserAccount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, UserAccount> users = new LinkedHashMap<>();

    @Override
    public void initialize() {
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public void save(UserAccount userAccount) {
        users.put(normalize(userAccount.getUsername()), userAccount);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(normalize(username)));
    }

    @Override
    public List<UserAccount> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        return users.values().stream()
                .filter(user -> user.getRole() == role)
                .collect(Collectors.toList());
    }

    @Override
    public UserAccount delete(String username) {
        UserAccount removed = users.remove(normalize(username));
        if (removed == null) {
            throw new RuntimeException("user not found: " + username);
        }
        return removed;
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase();
    }
}
