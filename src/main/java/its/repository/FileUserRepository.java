package its.repository;

import its.model.Role;
import its.model.UserAccount;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileUserRepository implements UserRepository {
    private final IssueRepository issueRepository;

    public FileUserRepository(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public void save(UserAccount userAccount) {
        IssueStore store = issueRepository.load();
        deleteIfPresent(store, userAccount.getUsername());
        store.getUsers().add(userAccount);
        issueRepository.save(store);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return issueRepository.load().getUsers().stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }

    @Override
    public List<UserAccount> findAll() {
        return List.copyOf(issueRepository.load().getUsers());
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        return issueRepository.load().getUsers().stream()
                .filter(user -> user.getRole() == role)
                .collect(Collectors.toList());
    }

    @Override
    public UserAccount delete(String username) {
        IssueStore store = issueRepository.load();
        UserAccount user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("user not found: " + username));
        deleteIfPresent(store, username);
        issueRepository.save(store);
        return user;
    }

    private void deleteIfPresent(IssueStore store, String username) {
        store.getUsers().removeIf(user -> user.getUsername().equalsIgnoreCase(username.trim()));
    }
}
