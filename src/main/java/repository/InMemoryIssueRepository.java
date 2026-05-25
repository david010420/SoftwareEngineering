package repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import model.Issue;
import model.IssueComment;
import model.IssueNotFoundException;
import model.IssueStatus;
import model.Priority;

public class InMemoryIssueRepository implements IssueRepository {
    private final Map<Long, Issue> issues = new LinkedHashMap<>();
    private long nextIssueId = 1L;
    private long nextCommentId = 1L;

    @Override
    public void initialize() {
    }

    @Override
    public Issue save(Issue issue) {
        if (issue.getId() == 0L) {
            issue.setId(nextIssueId++);
        }
        issues.put(issue.getId(), issue);
        return issue;
    }

    @Override
    public void update(Issue issue) {
        issues.put(issue.getId(), issue);
    }

    @Override
    public Optional<Issue> findById(long id) {
        return Optional.ofNullable(issues.get(id));
    }

    @Override
    public List<Issue> findAll() {
        return issues.values().stream()
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> findByProjectId(long projectId) {
        return issues.values().stream()
                .filter(issue -> issue.getProjectId() == projectId)
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> findByReporterUsername(String reporterUsername) {
        return issues.values().stream()
                .filter(issue -> issue.getReporterUsername().equals(reporterUsername))
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> findByAssigneeUsername(String assigneeUsername) {
        return issues.values().stream()
                .filter(issue -> assigneeUsername != null && assigneeUsername.equals(issue.getAssigneeUsername()))
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> findByStatus(IssueStatus status) {
        return issues.values().stream()
                .filter(issue -> issue.getStatus() == status)
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> findByPriority(Priority priority) {
        return issues.values().stream()
                .filter(issue -> issue.getPriority() == priority)
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Issue> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll();
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        return issues.values().stream()
                .filter(issue -> (issue.getTitle() + " " + issue.getDescription()).toLowerCase().contains(normalizedKeyword))
                .sorted(Comparator.comparing(Issue::getReportedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public IssueComment addComment(long issueId, IssueComment comment) {
        Issue issue = findById(issueId).orElseThrow(() -> new IssueNotFoundException(issueId));
        if (comment.getIssueId() != issueId) {
            throw new IllegalArgumentException("Comment issueId does not match target issue.");
        }
        comment.setId(nextCommentId++);
        issue.addComment(comment);
        update(issue);
        return comment;
    }

    @Override
    public List<IssueComment> findCommentsByIssueId(long issueId) {
        return findById(issueId).map(Issue::getComments).orElseGet(ArrayList::new);
    }

    @Override
    public Map<LocalDate, Long> countReportedByDay() {
        return issues.values().stream()
                .collect(Collectors.groupingBy(issue -> issue.getReportedAt().toLocalDate(), LinkedHashMap::new, Collectors.counting()));
    }

    @Override
    public Map<YearMonth, Long> countReportedByMonth() {
        return issues.values().stream()
                .collect(Collectors.groupingBy(issue -> YearMonth.from(issue.getReportedAt()), LinkedHashMap::new, Collectors.counting()));
    }
}
