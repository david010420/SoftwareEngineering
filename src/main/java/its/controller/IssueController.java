package its.controller;

import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Project;
import its.model.Role;
import its.model.UserAccount;
import its.service.IssueSearchCriteria;
import its.service.IssueService;
import its.service.IssueStatistics;

import java.util.List;

public class IssueController {
    private final IssueService service;

    public IssueController(IssueService service) {
        this.service = service;
    }

    public void seedDemoData() {
        service.seedDemoData();
    }

    public List<Project> projects() {
        return service.listProjects();
    }

    public List<UserAccount> users() {
        return service.listUsers();
    }

    public UserAccount login(String username, String password) {
        return service.login(username, password);
    }

    public List<Issue> issues() {
        return service.listIssues();
    }

    public Project addProject(String name) {
        return service.addProject(name);
    }

    public UserAccount addUser(String username, Role role) {
        return service.addUser(username, role);
    }

    public Issue createIssue(String projectName, String title, String description, String reporter, Priority priority) {
        return service.createIssue(projectName, title, description, reporter, priority);
    }

    public Issue addComment(long issueId, String author, String message) {
        return service.addComment(issueId, author, message);
    }

    public Issue assignIssue(long issueId, String assignee, String actor, String comment) {
        return service.assignIssue(issueId, assignee, actor, comment);
    }

    public Issue markFixed(long issueId, String fixer, String comment) {
        return service.markFixed(issueId, fixer, comment);
    }

    public Issue changeStatus(long issueId, IssueStatus status, String actor, String comment) {
        return service.changeStatus(issueId, status, actor, comment);
    }

    public List<Issue> search(IssueSearchCriteria criteria) {
        return service.search(criteria);
    }

    public IssueStatistics statistics() {
        return service.statistics();
    }

    public List<String> recommendAssignees(long issueId) {
        return service.recommendAssignees(issueId);
    }
}
