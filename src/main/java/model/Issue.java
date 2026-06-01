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

    public Issue(long id, long projectId, String title, String description, String reporterUsername,
                 LocalDateTime reportedAt, String fixerUsername, String assigneeUsername,
                 Priority priority, IssueStatus status, List<IssueComment> comments) {
        this.id = id;
        this.projectId = projectId;
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.reporterUsername = requireText(reporterUsername, "reporterUsername");
        this.reportedAt = Objects.requireNonNull(reportedAt, "reportedAt");
        this.fixerUsername = normalizeNullable(fixerUsername);
        this.assigneeUsername = normalizeNullable(assigneeUsername);
        this.priority = priority == null ? Priority.MAJOR : priority;
        this.status = status == null ? IssueStatus.NEW : status;
        this.comments = new ArrayList<>(comments == null ? Collections.emptyList() : comments);
    }

    // 신규 이슈 등록용 팩토리: id는 저장 시 채워지고 상태는 NEW로 시작한다.
    public static Issue report(long projectId, String title, String description, String reporterUsername, Priority priority) {
        return new Issue(0L, projectId, title, description, reporterUsername,
                LocalDateTime.now(), null, null, priority, IssueStatus.NEW, new ArrayList<>());
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReporterUsername() {
        return reporterUsername;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public String getFixerUsername() {
        return fixerUsername;
    }

    public void setFixerUsername(String fixerUsername) {
        this.fixerUsername = normalizeNullable(fixerUsername);
    }

    public String getAssigneeUsername() {
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

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String toString() {
        return "#" + id + " [" + status + "] " + title;
    }
}
