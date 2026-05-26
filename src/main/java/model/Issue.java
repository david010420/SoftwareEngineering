package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Issue implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private String projectName;

    private String projectId;
    private String title;
    private String description;
    private String reporter;
    private LocalDateTime reportedDate;
    private String fixer;
    private String assignee;
    private Priority priority;
    private IssueStatus status;
    private final List<Comment> comments = new ArrayList<>();

    public Issue(long id, String projectName, String projectId, String title, String description, String reporter,
                 LocalDateTime reportedDate, Priority priority) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        this.id = id;
        this.projectName = requireText(projectName, "projectName");
        this.projectId = projectId;
        this.title = title.trim();
        this.description = description.trim();
        this.reporter = requireText(reporter, "reporter");
        this.reportedDate = Objects.requireNonNull(reportedDate, "reportedDate");
        this.priority = priority == null ? Priority.MAJOR : priority;
        this.status = IssueStatus.NEW;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public long getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReporter() {
        return reporter;
    }

    public LocalDateTime getReportedDate() {
        return reportedDate;
    }

    public String getFixer() {
        return fixer;
    }

    public String getAssignee() {
        return assignee;
    }

    public Priority getPriority() {
        return priority;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public List<Comment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public void assignTo(String assignee) {
        this.assignee = requireText(assignee, "assignee");
        this.status = IssueStatus.ASSIGNED;
    }

    public void markFixed(String fixer) {
        this.fixer = requireText(fixer, "fixer");
        this.status = IssueStatus.FIXED;
    }

    public void changeStatus(IssueStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void addComment(Comment comment) {
        comments.add(Objects.requireNonNull(comment, "comment"));
    }

    public boolean containsText(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase();
        return title.toLowerCase().contains(q)
                || description.toLowerCase().contains(q)
                || comments.stream().anyMatch(c -> c.getMessage().toLowerCase().contains(q));
    }

    @Override
    public String toString() {
        return "#" + id + " [" + status + "] " + title;
    }
}
