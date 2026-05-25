package repository;

import model.Issue;
import model.IssueComment;
import model.IssueNotFoundException;
import model.IssueStatus;
import model.Priority;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileIssueRepository implements IssueRepository {
    private final Path path;

    public FileIssueRepository(Path path) {
        this.path = path;
    }

    @Override
    public void initialize() {
        FileStore.save(path, FileStore.load(path));
    }

    @Override
    public Issue save(Issue issue) {
        FileStore store = FileStore.load(path);
        if (issue.getId() == 0L) {
            issue.setId(nextIssueId(store));
        }
        store.issues().removeIf(existing -> existing.getId() == issue.getId());
        store.issues().add(issue);
        FileStore.save(path, store);
        return issue;
    }

    @Override
    public void update(Issue issue) {
        save(issue);
    }

    @Override
    public Optional<Issue> findById(long id) {
        return FileStore.load(path).issues().stream()
                .filter(issue -> issue.getId() == id)
                .findFirst();
    }

    @Override
    public List<Issue> findAll() {
        return sorted(FileStore.load(path).issues());
    }

    @Override
    public List<Issue> findByProjectId(long projectId) {
        return sorted(FileStore.load(path).issues().stream()
                .filter(issue -> issue.getProjectId() == projectId)
                .toList());
    }

    @Override
    public List<Issue> findByReporterUsername(String reporterUsername) {
        return sorted(FileStore.load(path).issues().stream()
                .filter(issue -> issue.getReporterUsername().equals(reporterUsername))
                .toList());
    }

    @Override
    public List<Issue> findByAssigneeUsername(String assigneeUsername) {
        return sorted(FileStore.load(path).issues().stream()
                .filter(issue -> assigneeUsername != null && assigneeUsername.equals(issue.getAssigneeUsername()))
                .toList());
    }

    @Override
    public List<Issue> findByStatus(IssueStatus status) {
        return sorted(FileStore.load(path).issues().stream()
                .filter(issue -> issue.getStatus() == status)
                .toList());
    }

    @Override
    public List<Issue> findByPriority(Priority priority) {
        return sorted(FileStore.load(path).issues().stream()
                .filter(issue -> issue.getPriority() == priority)
                .toList());
    }

    @Override
    public List<Issue> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        return sorted(FileStore.load(path).issues().stream()
                .filter(issue -> (issue.getTitle() + " " + issue.getDescription()).toLowerCase().contains(normalizedKeyword))
                .toList());
    }

    @Override
    public IssueComment addComment(long issueId, IssueComment comment) {
        FileStore store = FileStore.load(path);
        Issue issue = store.issues().stream()
                .filter(existing -> existing.getId() == issueId)
                .findFirst()
                .orElseThrow(() -> new IssueNotFoundException(issueId));
        comment.setId(nextCommentId(store));
        issue.addComment(comment);
        FileStore.save(path, store);
        return comment;
    }

    @Override
    public List<IssueComment> findCommentsByIssueId(long issueId) {
        return findById(issueId)
                .map(Issue::getComments)
                .map(ArrayList::new)
                .orElseGet(ArrayList::new);
    }

    @Override
    public Map<LocalDate, Long> countReportedByDay() {
        return FileStore.load(path).issues().stream()
                .collect(Collectors.groupingBy(issue -> issue.getReportedAt().toLocalDate(), LinkedHashMap::new, Collectors.counting()));
    }

    @Override
    public Map<YearMonth, Long> countReportedByMonth() {
        return FileStore.load(path).issues().stream()
                .collect(Collectors.groupingBy(issue -> YearMonth.from(issue.getReportedAt()), LinkedHashMap::new, Collectors.counting()));
    }

    private List<Issue> sorted(List<Issue> issues) {
        return issues.stream()
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .toList();
    }

    private long nextIssueId(FileStore store) {
        return store.issues().stream()
                .mapToLong(Issue::getId)
                .max()
                .orElse(0L) + 1L;
    }

    private long nextCommentId(FileStore store) {
        return store.issues().stream()
                .flatMap(issue -> issue.getComments().stream())
                .mapToLong(IssueComment::getId)
                .max()
                .orElse(0L) + 1L;
    }
}
