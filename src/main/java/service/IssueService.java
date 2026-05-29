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
import repository.IssueRepository;
import repository.ProjectRepository;

public class IssueService {
    private final IssueRepository repository;
    private final ProjectRepository projectRepository;

    public IssueService(IssueRepository repository) {
        this(repository, null);
    }

    public IssueService(IssueRepository repository, ProjectRepository projectRepository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.projectRepository = projectRepository;
    }

    public Issue reportIssue(long projectId, String title, String description, String reporterUsername, Priority priority) {
        Issue issue = Issue.report(projectId, title, description, reporterUsername, priority);
        requireExistingProject(issue.getProjectId());
        return repository.save(issue);
    }

    public Issue getIssue(long issueId) {
        Issue issue = repository.findById(issueId).orElseThrow(() -> new IssueNotFoundException(issueId));
        issue.setComments(repository.findCommentsByIssueId(issueId));
        return issue;
    }

    public List<Issue> findAllIssues() {
        return repository.findAll();
    }

    public List<Issue> findIssuesByProjectId(long projectId) {
        return repository.findByProjectId(projectId);
    }

    public List<Issue> findIssuesByReporterUsername(String reporterUsername) {
        return repository.findByReporterUsername(reporterUsername);
    }

    public List<Issue> findIssuesByAssigneeUsername(String assigneeUsername) {
        return repository.findByAssigneeUsername(assigneeUsername);
    }

    public List<Issue> findIssuesByStatus(IssueStatus status) {
        return repository.findByStatus(status);
    }

    public List<Issue> findIssuesByPriority(Priority priority) {
        return repository.findByPriority(priority);
    }

    public List<Issue> searchIssuesByKeyword(String keyword) {
        return repository.searchByKeyword(keyword);
    }

    public IssueComment addComment(long issueId, String authorUsername, String body) {
        return repository.addComment(issueId, IssueComment.create(issueId, authorUsername, body));
    }

    public Issue assignIssue(long issueId, String assigneeUsername, String actorUsername, String commentBody) {
        String assignee = requireText(assigneeUsername, "assigneeUsername");
        Issue issue = getIssue(issueId);
        if (issue.getStatus() != IssueStatus.NEW && issue.getStatus() != IssueStatus.REOPENED) {
            throw new IllegalStateException("Only NEW or REOPENED issues can be assigned.");
        }

        issue.setAssigneeUsername(assignee);
        issue.setStatus(IssueStatus.ASSIGNED);
        repository.update(issue);
        addCommentIfPresent(issueId, actorUsername, commentBody);
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

    private void requireExistingProject(long projectId) {
        if (projectRepository != null && !projectRepository.exists(projectId)) {
            throw new IllegalArgumentException("Unknown project: " + projectId);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
