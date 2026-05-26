package model;

import java.io.Serializable;
import java.util.Objects;

public class UserAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String username;
    private final String password;
    private final Role role;

    public UserAccount(long id, String username, String password, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        this.id = id;
        this.username = username.trim();
        this.password = password;
        this.role = Objects.requireNonNull(role, "role");
    }

    // id는 저장 시 채워지므로, 신규 계정 생성 시에는 0으로 위임한다.
    public UserAccount(String username, String password, Role role) {
        this(0L, username, password, role);
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public String getPassword() {
        return password;
    }

    // 로그인용
    public boolean checkPassword(String raw) {
        return password.equals(raw);
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
