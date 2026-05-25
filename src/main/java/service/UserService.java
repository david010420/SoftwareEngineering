package service;

import model.Role;
import model.UserAccount;

import java.util.List;

public interface UserService {
    void register(String requesterUsername, String newUsername, String password, Role role);

    UserAccount login(String username, String password);

    List<UserAccount> findAll();

    List<UserAccount> findByRole(Role role);

    UserAccount findByUsername(String username);

    void delete(String requesterUsername, String targetUsername);
}
