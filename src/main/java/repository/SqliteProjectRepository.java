package repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.Project;

public class SqliteProjectRepository implements ProjectRepository {
    private final Path databasePath;

    public SqliteProjectRepository(Path databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public void initialize() {
        try {
            Path parent = databasePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS projects ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "name TEXT NOT NULL,"
                        + "created_at TEXT NOT NULL"
                        + ")");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize SQLite project repository", exception);
        }
    }

    @Override
    public Project save(Project project) {
        String sql = "INSERT INTO projects(name, created_at) VALUES (?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, project.getName());
            statement.setString(2, project.getCreatedAt().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    project.setId(keys.getLong(1));
                    return project;
                }
            }
            throw new IllegalStateException("SQLite did not return generated project id");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save project", exception);
        }
    }

    @Override
    public Optional<Project> findById(long id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProject(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find project", exception);
        }
    }

    @Override
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY id ASC";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Project> projects = new ArrayList<>();
            while (resultSet.next()) {
                projects.add(mapProject(resultSet));
            }
            return projects;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find projects", exception);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }

    private static Project mapProject(ResultSet resultSet) throws SQLException {
        return new Project(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }
}
