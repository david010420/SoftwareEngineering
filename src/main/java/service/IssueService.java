package service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import model.Issue;
import model.IssueComment;
import model.IssueNotFoundException;
import model.IssueStatus;
import model.Priority;
import model.Role;
import model.UserAccount;
import repository.IssueRepository;
import repository.UserRepository;

public class IssueService {
    private final IssueRepository repository;
    private final UserRepository userRepository;

    public IssueService(IssueRepository repository) {
        this(repository, null);
    }

    public IssueService(IssueRepository repository, UserRepository userRepository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.userRepository = userRepository;
    }

    public Issue getIssue(long issueId) {
        Issue issue = repository.findById(issueId).orElseThrow(() -> new IssueNotFoundException(issueId));
        issue.setComments(repository.findCommentsByIssueId(issueId));
        return issue;
    }

    public List<Issue> findAllIssues() {
        return repository.findAll();
    }

    public IssueComment addComment(long issueId, String authorUsername, String body) {
        Issue issue = getIssue(issueId);
        IssueComment comment = IssueComment.create(issueId, authorUsername, body);
        issue.addComment(comment);
        return repository.addComment(issueId, comment);
    }

    public Issue assignIssue(long issueId, String assigneeUsername, String actorUsername, String commentBody) {
        String assignee = requireText(assigneeUsername, "assigneeUsername");
        String comment = requireText(commentBody, "commentBody");
        requireDeveloperAssignee(assignee);

        Issue issue = getIssue(issueId);
        if (issue.getStatus() != IssueStatus.NEW && issue.getStatus() != IssueStatus.REOPENED) {
            throw new IllegalStateException("Only NEW or REOPENED issues can be assigned.");
        }

        // UC-03 <<include>> UC-04: assignment must always add an issue comment before completing assign.
        addComment(issueId, actorUsername, comment);
        issue.setAssigneeUsername(assignee);
        issue.setStatus(IssueStatus.ASSIGNED);
        repository.update(issue);
        return getIssue(issueId);
    }

    public Issue markFixed(long issueId, String fixerUsername, String commentBody) {
        String fixer = requireText(fixerUsername, "fixerUsername");
        Issue issue = getIssue(issueId);
        if (issue.getStatus() != IssueStatus.ASSIGNED && issue.getStatus() != IssueStatus.REOPENED) {
            throw new IllegalStateException("Only ASSIGNED or REOPENED issues can be fixed.");
        }
        if (issue.getAssigneeUsername() != null && !issue.getAssigneeUsername().equals(fixer)) {
            throw new IllegalStateException("Only assigned developer can fix this issue.");
        }

        issue.setFixerUsername(fixer);
        issue.setStatus(IssueStatus.FIXED);
        repository.update(issue);
        addCommentIfPresent(issueId, fixer, commentBody);
        return getIssue(issueId);
    }

    public Issue resolveIssue(long issueId, String testerUsername, String commentBody) {
        Issue issue = getIssue(issueId);
        if (issue.getStatus() != IssueStatus.FIXED) {
            throw new IllegalStateException("Only FIXED issues can be resolved.");
        }

        issue.setStatus(IssueStatus.RESOLVED);
        repository.update(issue);
        addCommentIfPresent(issueId, testerUsername, commentBody);
        return getIssue(issueId);
    }

    public Issue closeIssue(long issueId, String actorUsername, String commentBody) {
        Issue issue = getIssue(issueId);
        if (issue.getStatus() != IssueStatus.RESOLVED) {
            throw new IllegalStateException("Only RESOLVED issues can be closed.");
        }

        issue.setStatus(IssueStatus.CLOSED);
        repository.update(issue);
        addCommentIfPresent(issueId, actorUsername, commentBody);
        return getIssue(issueId);
    }

    public Issue reopenIssue(long issueId, String actorUsername, String commentBody) {
        Issue issue = getIssue(issueId);
        if (issue.getStatus() != IssueStatus.RESOLVED && issue.getStatus() != IssueStatus.CLOSED) {
            throw new IllegalStateException("Only RESOLVED or CLOSED issues can be reopened.");
        }

        issue.setStatus(IssueStatus.REOPENED);
        repository.update(issue);
        addCommentIfPresent(issueId, actorUsername, commentBody);
        return getIssue(issueId);
    }

    public Map<LocalDate, Long> countReportedByDay() {
        return repository.countReportedByDay();
    }

    public Map<YearMonth, Long> countReportedByMonth() {
        return repository.countReportedByMonth();
    }

    public Map<IssueStatus, Long> countByStatus() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Issue::getStatus, LinkedHashMap::new, Collectors.counting()));
    }

    public Map<Priority, Long> countByPriority() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(Issue::getPriority, LinkedHashMap::new, Collectors.counting()));
    }

    public Map<String, Long> countByAssigneeUsername() {
        return repository.findAll().stream()
                .map(Issue::getAssigneeUsername)
                .filter(assignee -> assignee != null && !assignee.isBlank())
                .map(String::trim)
                .collect(Collectors.groupingBy(
                        String::toLowerCase,
                        Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private void addCommentIfPresent(long issueId, String authorUsername, String commentBody) {
        if (commentBody != null && !commentBody.trim().isEmpty()) {
            addComment(issueId, authorUsername, commentBody);
        }
    }

    private void requireDeveloperAssignee(String assigneeUsername) {
        if (userRepository == null) {
            return;
        }
        UserAccount assignee = userRepository.findByUsername(assigneeUsername)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user: " + assigneeUsername));
        if (assignee.getRole() != Role.DEV) {
            throw new IllegalArgumentException("Assignee must be a developer account: " + assigneeUsername);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
