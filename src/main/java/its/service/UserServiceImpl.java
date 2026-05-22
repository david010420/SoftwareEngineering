package its.service;

import its.model.Role;
import its.model.UserAccount;
import its.repository.UserRepository;

import java.util.List;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public void register(String requesterUsername,
                         String newUsername,
                         String password,
                         Role role) {
        requireRole(requesterUsername, Role.ADMIN);

        if (userRepository.existsByUsername(newUsername)) {
            throw new RuntimeException("user already exists: " + newUsername);
        }
        userRepository.save(new UserAccount(newUsername, password, role));
    }

    private void requireRole(String username, Role required) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("user not found: " + username));
        if (account.getRole() != required) {
            throw new RuntimeException(
                    username + " does not have role " + required);
        }
    }

    @Override
    public UserAccount login(String username, String password) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password."));

        if (!account.checkPassword(password)) {
            throw new RuntimeException("Invalid username or password.");
        }
        return account;
    }

    @Override
    public List<UserAccount> findAll() {
        return userRepository.findAll();
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    public UserAccount findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("user not found: " + username));
    }

    @Override
    public void delete(String requesterUsername, String targetUsername) {
        requireRole(requesterUsername, Role.ADMIN);
        findByUsername(targetUsername); // 존재 여부 확인
        userRepository.delete(targetUsername);
    }
}
