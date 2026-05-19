package its.service;

import its.model.Role;
import its.model.UserAccount;
import its.repository.UserRepository;

import java.util.List;

public class UserServiceImpl implements UserService{

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
            throw new RuntimeException(newUsername);
        }
        userRepository.save(new UserAccount(newUsername, password, role));
    }

    private void requireRole(String username, Role required) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(username));
        if (account.getRole() != required) {
            throw new RuntimeException(
                    username + " does not have role " + required);
        }
    }

    @Override
    public UserAccount login(String username, String password) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(username));

        if (!account.checkPassword(password)) {
            throw new RuntimeException();
        }
        return account;
    }

    @Override
    public List<UserAccount> findAll() {
        return null;
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        return null;
    }

    @Override
    public UserAccount findByUsername(String username) {
        return null;
    }

    @Override
    public void delete(String requesterUsername, String targetUsername) {

    }
}
