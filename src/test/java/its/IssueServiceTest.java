package its;

import its.controller.IssueController;
import its.controller.UserController;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Role;
import its.repository.FileUserRepository;
import its.repository.FileIssueRepository;
import its.service.IssueSearchCriteria;
import its.service.IssueService;
import its.service.UserServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;

public class IssueServiceTest {
    public static void main(String[] args) throws Exception {
        Path testStore = Path.of("data", "test-issues.store");
        Files.deleteIfExists(testStore);

        FileIssueRepository issueRepository = new FileIssueRepository(testStore);
        IssueController controller = new IssueController(new IssueService(issueRepository));
        UserController userController = new UserController(new UserServiceImpl(new FileUserRepository(issueRepository)));
        controller.addProject("project1");
        controller.addUser("admin", Role.ADMIN);
        controller.addUser("PL1", Role.PL);
        controller.addUser("dev1", Role.DEV);
        controller.addUser("tester1", Role.TESTER);
        assertEquals("tester1", userController.login("tester1", "1234").getUsername(), "user login succeeds");
        assertThrows(() -> userController.login("tester1", "wrong"), "user login rejects invalid password");
        userController.register("admin", "tester2", "1234", Role.TESTER);
        assertEquals("tester2", userController.findByUsername("tester2").getUsername(), "user register succeeds");

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

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
        } catch (RuntimeException e) {
            return;
        }
        throw new AssertionError(label + " expected exception");
    }
}
