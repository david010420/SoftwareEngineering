package repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.Issue;
import model.IssueComment;
import model.IssueNotFoundException;
import model.IssueStatus;
import model.Priority;

public class SqliteIssueRepository implements IssueRepository {
    private final Path databasePath;

    public SqliteIssueRepository(Path databasePath) {
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
                statement.executeUpdate("PRAGMA foreign_keys = ON");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS issues ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "project_id INTEGER NOT NULL,"
                        + "title TEXT NOT NULL,"
                        + "description TEXT NOT NULL,"
                        + "reporter_username TEXT NOT NULL,"
                        + "reported_at TEXT NOT NULL,"
                        + "fixer_username TEXT,"
                        + "assignee_username TEXT,"
                        + "priority TEXT NOT NULL,"
                        + "status TEXT NOT NULL"
                        + ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS issue_comments ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "issue_id INTEGER NOT NULL,"
                        + "author_username TEXT NOT NULL,"
                        + "body TEXT NOT NULL,"
                        + "created_at TEXT NOT NULL,"
                        + "FOREIGN KEY(issue_id) REFERENCES issues(id) ON DELETE CASCADE"
                        + ")");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize SQLite repository", exception);
        }
    }

    @Override
    public Issue save(Issue issue) {
        String sql = "INSERT INTO issues(project_id, title, description, reporter_username, reported_at, fixer_username, assignee_username, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindIssue(statement, issue);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    issue.setId(keys.getLong(1));
                    return issue;
                }
            }
            throw new IllegalStateException("SQLite did not return generated issue id");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save issue", exception);
        }
    }

    @Override
    public void update(Issue issue) {
        String sql = "UPDATE issues SET project_id = ?, title = ?, description = ?, reporter_username = ?, reported_at = ?, "
                + "fixer_username = ?, assignee_username = ?, priority = ?, status = ? WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bindIssue(statement, issue);
            statement.setLong(10, issue.getId());
            if (statement.executeUpdate() == 0) {
                throw new IssueNotFoundException(issue.getId());
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to update issue", exception);
        }
    }

    @Override
    public Optional<Issue> findById(long id) {
        String sql = "SELECT * FROM issues WHERE id = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Issue issue = mapIssue(resultSet, findCommentsByIssueId(id));
                    return Optional.of(issue);
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find issue", exception);
        }
    }

    @Override
    public List<Issue> findAll() {
        return findBySql("SELECT * FROM issues ORDER BY reported_at DESC");
    }

    @Override
    public List<Issue> findByProjectId(long projectId) {
        return findBySql("SELECT * FROM issues WHERE project_id = ? ORDER BY reported_at DESC", projectId);
    }

    @Override
    public List<Issue> findByReporterUsername(String reporterUsername) {
        return findBySql("SELECT * FROM issues WHERE reporter_username = ? ORDER BY reported_at DESC", reporterUsername);
    }

    @Override
    public List<Issue> findByAssigneeUsername(String assigneeUsername) {
        return findBySql("SELECT * FROM issues WHERE assignee_username = ? ORDER BY reported_at DESC", assigneeUsername);
    }

    @Override
    public List<Issue> findByStatus(IssueStatus status) {
        return findBySql("SELECT * FROM issues WHERE status = ? ORDER BY reported_at DESC", status.name());
    }

    @Override
    public List<Issue> findByPriority(Priority priority) {
        return findBySql("SELECT * FROM issues WHERE priority = ? ORDER BY reported_at DESC", priority.name());
    }

    @Override
    public List<Issue> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String keywordPattern = "%" + keyword.trim().toLowerCase() + "%";
        return findBySql("SELECT * FROM issues WHERE LOWER(title) LIKE ? OR LOWER(description) LIKE ? ORDER BY reported_at DESC",
                keywordPattern,
                keywordPattern);
    }

    @Override
    public IssueComment addComment(long issueId, IssueComment comment) {
        if (!findById(issueId).isPresent()) {
            throw new IssueNotFoundException(issueId);
        }
        if (comment.getIssueId() != issueId) {
            throw new IllegalArgumentException("Comment issueId does not match target issue.");
        }

        String sql = "INSERT INTO issue_comments(issue_id, author_username, body, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, issueId);
            statement.setString(2, comment.getAuthorUsername());
            statement.setString(3, comment.getBody());
            statement.setString(4, comment.getCreatedAt().toString());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    comment.setId(keys.getLong(1));
                    return comment;
                }
            }
            throw new IllegalStateException("SQLite did not return generated comment id");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to add issue comment", exception);
        }
    }

    @Override
    public List<IssueComment> findCommentsByIssueId(long issueId) {
        String sql = "SELECT * FROM issue_comments WHERE issue_id = ? ORDER BY created_at ASC, id ASC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, issueId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<IssueComment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(mapComment(resultSet));
                }
                return comments;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find comments", exception);
        }
    }

    @Override
    public Map<LocalDate, Long> countReportedByDay() {
        String sql = "SELECT substr(reported_at, 1, 10) AS bucket, COUNT(*) AS count FROM issues GROUP BY bucket ORDER BY bucket";
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.put(LocalDate.parse(resultSet.getString("bucket")), resultSet.getLong("count"));
            }
            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count issues by day", exception);
        }
    }

    @Override
    public Map<YearMonth, Long> countReportedByMonth() {
        String sql = "SELECT substr(reported_at, 1, 7) AS bucket, COUNT(*) AS count FROM issues GROUP BY bucket ORDER BY bucket";
        Map<YearMonth, Long> result = new LinkedHashMap<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.put(YearMonth.parse(resultSet.getString("bucket")), resultSet.getLong("count"));
            }
            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to count issues by month", exception);
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private static void bindIssue(PreparedStatement statement, Issue issue) throws SQLException {
        statement.setLong(1, issue.getProjectId());
        statement.setString(2, issue.getTitle());
        statement.setString(3, issue.getDescription());
        statement.setString(4, issue.getReporterUsername());
        statement.setString(5, issue.getReportedAt().toString());
        statement.setString(6, issue.getFixerUsername());
        statement.setString(7, issue.getAssigneeUsername());
        statement.setString(8, issue.getPriority().name());
        statement.setString(9, issue.getStatus().name());
    }

    private List<Issue> findBySql(String sql, Object... parameters) {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Issue> issues = new ArrayList<>();
                while (resultSet.next()) {
                    long issueId = resultSet.getLong("id");
                    issues.add(mapIssue(resultSet, findCommentsByIssueId(issueId)));
                }
                return issues;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to find issues", exception);
        }
    }

    private static Issue mapIssue(ResultSet resultSet, List<IssueComment> comments) throws SQLException {
        return new Issue(
                resultSet.getLong("id"),
                resultSet.getLong("project_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("reporter_username"),
                LocalDateTime.parse(resultSet.getString("reported_at")),
                resultSet.getString("fixer_username"),
                resultSet.getString("assignee_username"),
                Priority.valueOf(resultSet.getString("priority")),
                IssueStatus.valueOf(resultSet.getString("status")),
                comments
        );
    }

    private static IssueComment mapComment(ResultSet resultSet) throws SQLException {
        return new IssueComment(
                resultSet.getLong("id"),
                resultSet.getLong("issue_id"),
                resultSet.getString("author_username"),
                resultSet.getString("body"),
                LocalDateTime.parse(resultSet.getString("created_at"))
        );
    }
}
