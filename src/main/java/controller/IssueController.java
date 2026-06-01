package controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import model.Issue;
import model.IssueComment;
import model.IssueStatus;
import model.Priority;
import service.IssueService;
import service.IssueStatistics;
import service.ProjectService;
import service.RecommendService;

public class IssueController {
    private final IssueService service;
    private final ProjectService projectService;
    private final RecommendService recommendService;

    public IssueController(IssueService service, ProjectService projectService, RecommendService recommendService) {
        this.service = Objects.requireNonNull(service, "service");
        this.projectService = Objects.requireNonNull(projectService, "projectService");
        this.recommendService = Objects.requireNonNull(recommendService, "recommendService");
    }

    public Issue createIssue(long projectId, String title, String description, String reporterUsername, Priority priority) {
        return projectService.addIssue(projectId, title, description, reporterUsername, priority);
    }

    public Issue getIssue(long issueId) {
        return service.getIssue(issueId);
    }

    public List<Issue> issues() {
        return findAllIssues();
    }

    public List<Issue> findAllIssues() {
        return service.findAllIssues();
    }

    public List<Issue> search(String query, String reporterUsername, String assigneeUsername, IssueStatus status) {
        String normalizedQuery = normalize(query);
        String normalizedReporter = normalize(reporterUsername);
        String normalizedAssignee = normalize(assigneeUsername);

        return findAllIssues().stream()
                .filter(issue -> status == null || issue.getStatus() == status)
                .filter(issue -> normalizedReporter == null || normalizedReporter.equals(issue.getReporterUsername()))
                .filter(issue -> normalizedAssignee == null || normalizedAssignee.equals(issue.getAssigneeUsername()))
                .filter(issue -> normalizedQuery == null || containsKeyword(issue, normalizedQuery))
                .collect(Collectors.toList());
    }

    public IssueComment addComment(long issueId, String authorUsername, String body) {
        return service.addComment(issueId, authorUsername, body);
    }

    public Issue assignIssue(long issueId, String assigneeUsername, String actorUsername, String commentBody) {
        return service.assignIssue(issueId, assigneeUsername, actorUsername, commentBody);
    }

    public Issue markFixed(long issueId, String fixerUsername, String commentBody) {
        return service.markFixed(issueId, fixerUsername, commentBody);
    }

    public Issue changeStatus(long issueId, IssueStatus status, String actorUsername, String commentBody) {
        if (status == IssueStatus.RESOLVED) {
            return resolveIssue(issueId, actorUsername, commentBody);
        }
        if (status == IssueStatus.CLOSED) {
            return closeIssue(issueId, actorUsername, commentBody);
        }
        if (status == IssueStatus.REOPENED) {
            return reopenIssue(issueId, actorUsername, commentBody);
        }
        throw new IllegalArgumentException("Unsupported status change: " + status);
    }

    public Issue resolveIssue(long issueId, String testerUsername, String commentBody) {
        Issue issue = service.resolveIssue(issueId, testerUsername, commentBody);
        recommendService.cal(issue.getProjectId());
        return issue;
    }

    public Issue closeIssue(long issueId, String actorUsername, String commentBody) {
        Issue issue = service.closeIssue(issueId, actorUsername, commentBody);
        recommendService.cal(issue.getProjectId());
        return issue;
    }

    public Issue reopenIssue(long issueId, String actorUsername, String commentBody) {
        Issue issue = service.reopenIssue(issueId, actorUsername, commentBody);
        recommendService.cal(issue.getProjectId());
        return issue;
    }

    public void learnNow(long projectId) {
        recommendService.cal(projectId);
    }

    public Map<LocalDate, Long> countReportedByDay() {
        return service.countReportedByDay();
    }

    public Map<YearMonth, Long> countReportedByMonth() {
        return service.countReportedByMonth();
    }

    public IssueStatistics statistics() {
        return new IssueStatistics(
                countReportedByDay(),
                countReportedByMonth(),
                service.countByStatus(),
                service.countByPriority(),
                service.countByAssigneeUsername());
    }

    public List<String> recommendAssignees(long issueId) {
        return recommendService.recommendUser(issueId, 3);
    }

    private static boolean containsKeyword(Issue issue, String keyword) {
        String haystack = (issue.getTitle() + " " + issue.getDescription()).toLowerCase();
        return haystack.contains(keyword.toLowerCase());
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
