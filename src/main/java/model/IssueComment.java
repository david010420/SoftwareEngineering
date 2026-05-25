package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class IssueComment implements Serializable {
    private static final long serialVersionUID = 1L;
    private long id;
    private long issueId;
    private String authorUsername;
    private String body;
    private LocalDateTime createdAt;

    public IssueComment(long id, long issueId, String authorUsername, String body, LocalDateTime createdAt) {
        setId(id);
        this.issueId = issueId;
        this.authorUsername = requireText(authorUsername, "authorUsername");
        this.body = requireText(body, "body");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static IssueComment create(long issueId, String authorUsername, String body) {
        return new IssueComment(0L, issueId, authorUsername, body, LocalDateTime.now());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getIssueId() {
        return issueId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
