package its;

import its.controller.UserController;
import its.repository.FileIssueRepository;
import its.repository.FileUserRepository;
import its.service.UserServiceImpl;
import repository.InMemoryIssueRepository;
import repository.InMemoryProjectRepository;
import service.ProjectService;

import java.nio.file.Path;

public final class AppFactory {
    private AppFactory() {
    }

    public static its.controller.IssueController createController() {
        Path dataPath = Path.of("data", "issues.store");
        its.controller.IssueController controller =
                new its.controller.IssueController(new its.service.IssueService(new FileIssueRepository(dataPath)));
        controller.seedDemoData();
        return controller;
    }

    public static Controllers createControllers() {
        Path dataPath = Path.of("data", "issues.store");
        FileIssueRepository userBackingRepository = new FileIssueRepository(dataPath);
        its.controller.IssueController userSeedController =
                new its.controller.IssueController(new its.service.IssueService(userBackingRepository));
        userSeedController.seedDemoData();

        InMemoryProjectRepository projectRepository = new InMemoryProjectRepository();
        InMemoryIssueRepository issueRepository = new InMemoryIssueRepository();
        ProjectService projectService = new ProjectService(projectRepository, issueRepository);
        controller.IssueController issueController =
                new controller.IssueController(new service.IssueService(issueRepository, projectRepository), projectService);
        seedIssueBackendDemoData(issueController);

        UserController userController =
                new UserController(new UserServiceImpl(new FileUserRepository(userBackingRepository)));
        return new Controllers(issueController, userController);
    }

    private static void seedIssueBackendDemoData(controller.IssueController issueController) {
        model.Project project = issueController.addProject("project1");

        model.Issue loginIssue = issueController.reportIssue(project.getId(), "Login fails after password reset",
                "Users cannot sign in after resetting a password.", "tester1", model.Priority.CRITICAL);
        issueController.assignIssue(loginIssue.getId(), "dev1", "PL1", "Please check auth token refresh.");
        issueController.markFixed(loginIssue.getId(), "dev1", "Patched token refresh.");
        issueController.resolveIssue(loginIssue.getId(), "tester1", "Verified on test server.");
        issueController.closeIssue(loginIssue.getId(), "PL1", "Closed after verification.");

        model.Issue searchIssue = issueController.reportIssue(project.getId(), "Search filter ignores assignee",
                "Assignee filter returns all issues.", "tester2", model.Priority.MAJOR);
        issueController.assignIssue(searchIssue.getId(), "dev2", "PL1", "Please inspect search filtering.");
        issueController.markFixed(searchIssue.getId(), "dev2", "Fixed assignee condition.");
        issueController.resolveIssue(searchIssue.getId(), "tester2", "Verified filter result.");

        issueController.reportIssue(project.getId(), "Issue detail comment order is reversed",
                "Newest comment is shown before older comments.", "tester1", model.Priority.MINOR);
    }

    public static class Controllers {
        private final controller.IssueController issueController;
        private final UserController userController;

        private Controllers(controller.IssueController issueController, UserController userController) {
            this.issueController = issueController;
            this.userController = userController;
        }

        public controller.IssueController issueController() {
            return issueController;
        }

        public UserController userController() {
            return userController;
        }
    }
}
