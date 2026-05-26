package controller;

import model.Role;
import model.UserAccount;
import service.UserService;

import java.util.List;

public class UserController {

    private final UserService userService;

    public UserController(UserService service) {
        this.userService = service;
    }

    public void register(String requesterUsername, String newUsername, String password, Role role) {
        userService.register(requesterUsername, newUsername, password, role);
    }

    public UserAccount login(String username, String password) {
        return userService.login(username, password);
    }

    public List<UserAccount> findAll() {
        return userService.findAll();
    }

    public List<UserAccount> findByRole(Role role) {
        return userService.findByRole(role);
    }

    public UserAccount findByUsername(String username) {
        return userService.findByUsername(username);
    }

    public void delete(String requesterUsername, String targetUsername) {
        userService.delete(requesterUsername, targetUsername);
    }
}
