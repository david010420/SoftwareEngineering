package its.repository;

import its.model.Issue;
import its.model.Project;
import its.model.UserAccount;

import java.util.List;

public interface IssueRepository {
    IssueStore load();

    void save(IssueStore store);

    default List<Issue> issues() {
        return load().getIssues();
    }

    default List<UserAccount> users() {
        return load().getUsers();
    }

    default List<Project> projects() {
        return load().getProjects();
    }

    List<Issue> findByProjectId(String id);
}
