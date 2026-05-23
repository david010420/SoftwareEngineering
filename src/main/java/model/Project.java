package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Project {
    private long id;
    private String name;
    private LocalDateTime createdAt;
    private List<Issue> issues;

    public Project(long id, String name, LocalDateTime createdAt) {
        this(id, name, createdAt, Collections.emptyList());
    }

    public Project(long id, String name, LocalDateTime createdAt, List<Issue> issues) {
        setId(id);
        this.name = requireText(name, "name");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        setIssues(issues);
    }

    public static Project create(String name) {
        return new Project(0L, name, LocalDateTime.now());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Issue> getIssues() {
        return Collections.unmodifiableList(issues);
    }

    public void setIssues(List<Issue> issues) {
        this.issues = new ArrayList<>(issues == null ? Collections.emptyList() : issues);
    }

    public void addIssue(Issue issue) {
        issues.add(Objects.requireNonNull(issue, "issue"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return name;
    }
}
