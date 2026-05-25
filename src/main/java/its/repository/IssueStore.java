package its.repository;

import its.model.Issue;
import its.model.Project;
import its.model.UserAccount;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IssueStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private long nextIssueId = 1L;
    private long nextUserId = 1L;
    private final List<Project> projects = new ArrayList<>();
    private final List<UserAccount> users = new ArrayList<>();
    private final List<Issue> issues = new ArrayList<>();

    public long nextIssueId() {
        return nextIssueId++;
    }

    public void setNextIssueId(long nextId) {
        this.nextIssueId = nextId;
    }

    public long nextUserId() {
        if (nextUserId == 0L) {
            nextUserId = 1L;
        }
        return nextUserId++;
    }

    public void setNextUserId(long nextId) {
        this.nextUserId = nextId;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public List<UserAccount> getUsers() {
        return users;
    }

    public List<Issue> getIssues() {
        return issues;
    }
}
