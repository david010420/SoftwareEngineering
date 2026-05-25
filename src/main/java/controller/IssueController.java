package controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import model.Issue;
import model.IssueComment;
import model.IssueStatus;
import model.Priority;
import model.Project;
import service.IssueService;
import service.IssueStatistics;
import service.ProjectService;
import service.RecommendService;

public class IssueController {
    private final IssueService service;
    private final ProjectService projectService;
    private final RecommendService recommendService;

    public IssueController(IssueService service) {
        this(service, null, null);
    }

    public IssueController(IssueService service, ProjectService projectService) {
        this(service, projectService, null);
    }

    public IssueController(IssueService service, ProjectService projectService, RecommendService recommendService) {
        this.service = Objects.requireNonNull(service, "service");
        this.projectService = projectService;
        this.recommendService = recommendService;
    }

    public Issue reportIssue(long projectId, String title, String description, String reporterUsername, Priority priority) {
        return service.reportIssue(projectId, title, description, reporterUsername, priority);
    }

    public Issue createIssue(String projectName, String title, String description, String reporterUsername, Priority priority) {
        Project project = findProjectByName(projectName);
        return reportIssue(project.getId(), title, description, reporterUsername, priority);
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

    public List<Project> projects() {
        if (projectService == null) {
            return Collections.emptyList();
        }
        return projectService.findAllProjects();
    }

    public Project addProject(String name) {
        requireProjectService();
        return projectService.createProject(name);
    }

    public List<Issue> findIssuesByProjectId(long projectId) {
        return service.findIssuesByProjectId(projectId);
    }

    public List<Issue> findIssuesByReporterUsername(String reporterUsername) {
        return service.findIssuesByReporterUsername(reporterUsername);
    }

    public List<Issue> findIssuesByAssigneeUsername(String assigneeUsername) {
        return service.findIssuesByAssigneeUsername(assigneeUsername);
    }

    public List<Issue> findIssuesByStatus(IssueStatus status) {
        return service.findIssuesByStatus(status);
    }

    public List<Issue> findIssuesByPriority(Priority priority) {
        return service.findIssuesByPriority(priority);
    }

    public List<Issue> searchIssuesByKeyword(String keyword) {
        return service.searchIssuesByKeyword(keyword);
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
        return service.resolveIssue(issueId, testerUsername, commentBody);
    }

    public Issue closeIssue(long issueId, String actorUsername, String commentBody) {
        return service.closeIssue(issueId, actorUsername, commentBody);
    }

    public Issue reopenIssue(long issueId, String actorUsername, String commentBody) {
        return service.reopenIssue(issueId, actorUsername, commentBody);
    }

    public Map<LocalDate, Long> countReportedByDay() {
        return service.countReportedByDay();
    }

    public Map<YearMonth, Long> countReportedByMonth() {
        return service.countReportedByMonth();
    }

    public IssueStatistics statistics() {
        return new IssueStatistics(countReportedByDay(), countReportedByMonth());
    }

    public List<String> recommendAssignees(long issueId) {
        if (recommendService == null) {
            return Collections.emptyList();
        }
        return recommendService.recommendUser(issueId, 3);
    }

    private Project findProjectByName(String projectName) {
        requireProjectService();
        String normalizedProjectName = normalize(projectName);
        if (normalizedProjectName == null) {
            throw new IllegalArgumentException("projectName is required");
        }
        return projectService.findAllProjects().stream()
                .filter(project -> project.getName().equalsIgnoreCase(normalizedProjectName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown project: " + projectName));
    }

    private void requireProjectService() {
        if (projectService == null) {
            throw new IllegalStateException("ProjectService is required for project operations.");
        }
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
