package its.repository;

import its.model.Role;
import its.model.UserAccount;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {

    private final String url;

    public SqliteUserRepository(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public boolean existsByUsername(String username) {
        final String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
    }

    @Override
    public void save(UserAccount user) {
        final String sql = "INSERT OR REPLACE INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        final String sql = "SELECT username, password, role FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
    }

    @Override
    public List<UserAccount> findAll() {
        final String sql = "SELECT username, password, role FROM users ORDER BY username";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<UserAccount> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        final String sql = "SELECT username, password, role FROM users WHERE role = ? ORDER BY username";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<UserAccount> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
    }

    @Override
    public UserAccount delete(String username) {
        UserAccount user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자 없음: " + username));

        final String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
        return user;
    }

    private UserAccount mapRow(ResultSet rs) throws SQLException {
        return new UserAccount(
                rs.getString("username"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role")));
    }
}
