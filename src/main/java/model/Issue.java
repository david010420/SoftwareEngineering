package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Issue implements Serializable {
    private static final long serialVersionUID = 1L;
    private long id;
    private long projectId;
    private String title;
    private String description;
    private String reporterUsername;
    private LocalDateTime reportedAt;
    private String fixerUsername;
    private String assigneeUsername;
    private Priority priority;
    private IssueStatus status;
    private List<IssueComment> comments;

    public Issue(
            long id,
            long projectId,
            String title,
            String description,
            String reporterUsername,
            LocalDateTime reportedAt,
            String fixerUsername,
            String assigneeUsername,
            Priority priority,
            IssueStatus status,
            List<IssueComment> comments
    ) {
        setId(id);
        this.projectId = requirePositiveId(projectId, "projectId");
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.reporterUsername = requireText(reporterUsername, "reporterUsername");
        this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt");
        setFixerUsername(fixerUsername);
        setAssigneeUsername(assigneeUsername);
        this.priority = Objects.requireNonNull(priority, "priority");
        setStatus(status);
        setComments(comments);
    }

    public static Issue report(long projectId, String title, String description, String reporterUsername, Priority priority) {
        return new Issue(
                0L,
                projectId,
                title,
                description,
                reporterUsername,
                LocalDateTime.now(),
                null,
                null,
                priority == null ? Priority.MAJOR : priority,
                IssueStatus.NEW,
                Collections.emptyList()
        );
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return String.valueOf(projectId);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public String getReporter() {
        return reporterUsername;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public LocalDateTime getReportedDate() {
        return reportedAt;
    }

    public String getFixerUsername() {
        return fixerUsername;
    }

    public String getFixer() {
        return fixerUsername;
    }

    public void setFixerUsername(String fixerUsername) {
        this.fixerUsername = normalizeNullable(fixerUsername);
    }

    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    public String getAssignee() {
        return assigneeUsername;
    }

    public void setAssigneeUsername(String assigneeUsername) {
        this.assigneeUsername = normalizeNullable(assigneeUsername);
    }

    public Priority getPriority() {
        return priority;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public void setStatus(IssueStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public List<IssueComment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public void setComments(List<IssueComment> comments) {
        this.comments = new ArrayList<>(comments == null ? Collections.emptyList() : comments);
    }

    public void addComment(IssueComment comment) {
        comments.add(Objects.requireNonNull(comment, "comment"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static long requirePositiveId(long value, String fieldName) {
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "#" + id + " [" + status + "] " + title;
    }
}
