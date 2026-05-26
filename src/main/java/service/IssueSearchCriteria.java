package service;

import model.IssueStatus;

public class IssueSearchCriteria {
    private String projectName;
    private String reporter;
    private String assignee;
    private IssueStatus status;
    private String query;

    public String getProjectName() {
        return projectName;
    }

    public IssueSearchCriteria projectName(String projectName) {
        this.projectName = blankToNull(projectName);
        return this;
    }

    public String getReporter() {
        return reporter;
    }

    public IssueSearchCriteria reporter(String reporter) {
        this.reporter = blankToNull(reporter);
        return this;
    }

    public String getAssignee() {
        return assignee;
    }

    public IssueSearchCriteria assignee(String assignee) {
        this.assignee = blankToNull(assignee);
        return this;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public IssueSearchCriteria status(IssueStatus status) {
        this.status = status;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public IssueSearchCriteria query(String query) {
        this.query = blankToNull(query);
        return this;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
