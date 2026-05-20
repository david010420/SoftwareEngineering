package its.repository;

import its.model.Role;
import its.model.UserAccount;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MysqlUserRepository implements UserRepository {

    private final DataSource dataSource;

    public MysqlUserRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean existsByUsername(String username) {
        final String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error: existsByUsername", e);
        }
    }

    @Override
    public void save(UserAccount user) {
        final String sql = """
                INSERT INTO users (username, password, role)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    password = VALUES(password),
                    role     = VALUES(role)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error: save", e);
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        final String sql =
                "SELECT username, password, role FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error: findByUsername", e);
        }
    }

    @Override
    public List<UserAccount> findAll() {
        final String sql =
                "SELECT username, password, role FROM users ORDER BY username";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<UserAccount> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("DB error: findAll", e);
        }
    }

    @Override
    public List<UserAccount> findByRole(Role role) {
        final String sql =
                "SELECT username, password, role FROM users WHERE role = ? ORDER BY username";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<UserAccount> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error: findByRole", e);
        }
    }

    @Override
    public UserAccount delete(String username) {
        // 삭제 전에 객체를 먼저 조회해서 갖고 있기
        UserAccount user = findByUsername(username)
                .orElseThrow(() -> new RuntimeException(username));

        final String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("DB error: delete", e);
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
