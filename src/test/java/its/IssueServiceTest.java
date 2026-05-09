package its;

import its.controller.IssueController;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Role;
import its.repository.FileIssueRepository;
import its.service.IssueSearchCriteria;
import its.service.IssueService;

import java.nio.file.Files;
import java.nio.file.Path;

public class IssueServiceTest {
    public static void main(String[] args) throws Exception {
        Path testStore = Path.of("data", "test-issues.store");
        Files.deleteIfExists(testStore);

        IssueController controller = new IssueController(new IssueService(new FileIssueRepository(testStore)));
        controller.addProject("project1");
        controller.addUser("PL1", Role.PL);
        controller.addUser("dev1", Role.DEV);
        controller.addUser("tester1", Role.TESTER);

        Issue issue = controller.createIssue("project1", "Login error", "Cannot login with valid account", "tester1", Priority.MAJOR);
        assertEquals(IssueStatus.NEW, issue.getStatus(), "new issue status");
        assertEquals("tester1", issue.getReporter(), "reporter auto set");

        controller.addComment(issue.getId(), "tester1", "Reproduced in Chrome.");
        controller.assignIssue(issue.getId(), "dev1", "PL1", "Please fix this.");
        controller.markFixed(issue.getId(), "dev1", "Patched token validation.");
        controller.changeStatus(issue.getId(), IssueStatus.RESOLVED, "tester1", "Verified.");

        Issue loaded = controller.search(new IssueSearchCriteria().assignee("dev1").status(IssueStatus.RESOLVED)).get(0);
        assertEquals("dev1", loaded.getFixer(), "fixer registered");
        assertEquals(4, loaded.getComments().size(), "comment history accumulated");
        assertEquals(1L, controller.statistics().getDailyCounts().values().stream().mapToLong(Long::longValue).sum(), "daily stats");

        Files.deleteIfExists(testStore);
        System.out.println("IssueServiceTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
    }
}
