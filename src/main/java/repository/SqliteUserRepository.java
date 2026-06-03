package repository;

import model.Role;
import model.UserAccount;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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
    public void initialize() {
        final String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT NOT NULL UNIQUE,"
                + "password TEXT NOT NULL,"
                + "role TEXT NOT NULL"
                + ")";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
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
        final String sql = "INSERT OR REPLACE INTO users (id, username, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (user.getId() == 0L) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setLong(1, user.getId());
            }
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB 오류: ", e);
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        final String sql = "SELECT id, username, password, role FROM users WHERE username = ?";
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
        final String sql = "SELECT id, username, password, role FROM users ORDER BY username";
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
        final String sql = "SELECT id, username, password, role FROM users WHERE role = ? ORDER BY username";
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
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role")));
    }
}
