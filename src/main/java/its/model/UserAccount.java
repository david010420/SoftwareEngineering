package its.model;

import java.io.Serializable;
import java.util.Objects;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_PASSWORD = "1234";

    private final String username;
    private final String password;
    private final Role role;

    public UserAccount(String username, Role role) {
        this(username, DEFAULT_PASSWORD, role);
    }

    public UserAccount(String username, String password, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        this.username = username.trim();
        this.password = password;
        this.role = Objects.requireNonNull(role, "role");
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getPassword() {
        return password == null ? DEFAULT_PASSWORD : password;
    }

    public boolean checkPassword(String raw) {
        return getPassword().equals(raw);
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
