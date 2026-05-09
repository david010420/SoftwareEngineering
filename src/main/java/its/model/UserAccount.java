package its.model;

import java.io.Serializable;
import java.util.Objects;

public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final Role role;

    public UserAccount(String username, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        this.username = username.trim();
        this.role = Objects.requireNonNull(role, "role");
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
