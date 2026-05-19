package its.model;

import java.io.Serializable;
import java.util.Objects;

public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;
    private final Role role;

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

    //로그인용
    public boolean checkPassword(String raw) {
        return password.equals(raw);
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
