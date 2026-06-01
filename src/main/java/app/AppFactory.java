package app;

import controller.ProjectController;
import controller.UserController;
import model.Role;
import model.UserAccount;
import repository.SqliteIssueRepository;
import repository.SqliteProjectRepository;
import repository.SqliteUserRepository;
import service.ProjectService;
import service.TFRecommendService;
import service.UserServiceImpl;

import java.nio.file.Path;

public final class AppFactory {
    private AppFactory() {
    }

    public static Controllers createControllers() {
        Path databasePath = Path.of("data", "its.db");
        SqliteProjectRepository projectRepository = new SqliteProjectRepository(databasePath);
        SqliteIssueRepository issueRepository = new SqliteIssueRepository(databasePath);
        SqliteUserRepository userRepository = new SqliteUserRepository(databasePath.toString());
        projectRepository.initialize();
        issueRepository.initialize();
        userRepository.initialize();

        ProjectService projectService = new ProjectService(projectRepository, issueRepository);
        ProjectController projectController = new ProjectController(projectService);
        controller.IssueController issueController =
                new controller.IssueController(
                        new service.IssueService(issueRepository, userRepository),
                        projectService,
                        new TFRecommendService(issueRepository));

        if (userRepository.findAll().isEmpty()) {
            seedDemoUsers(userRepository);
        }
        if (projectController.findAllProjects().isEmpty()) {
            seedIssueBackendDemoData(issueController, projectController);
        }

        UserController userController = new UserController(new UserServiceImpl(userRepository));
        return new Controllers(issueController, projectController, userController);
    }

    private static void seedDemoUsers(SqliteUserRepository userRepository) {
        userRepository.save(new UserAccount("admin", "1234", Role.ADMIN));
        userRepository.save(new UserAccount("PL1", "1234", Role.PL));
        userRepository.save(new UserAccount("PL2", "1234", Role.PL));
        userRepository.save(new UserAccount("dev1", "1234", Role.DEV));
        userRepository.save(new UserAccount("dev2", "1234", Role.DEV));
        userRepository.save(new UserAccount("tester1", "1234", Role.TESTER));
        userRepository.save(new UserAccount("tester2", "1234", Role.TESTER));
    }

    private static void seedIssueBackendDemoData(
            controller.IssueController issueController,
            ProjectController projectController) {
        model.Project project = projectController.createProject("project1");

        model.Issue loginIssue = issueController.createIssue(project.getId(), "Login fails after password reset",
                "Users cannot sign in after resetting a password.", "tester1", model.Priority.CRITICAL);
        issueController.assignIssue(loginIssue.getId(), "dev1", "PL1", "Please check auth token refresh.");
        issueController.markFixed(loginIssue.getId(), "dev1", "Patched token refresh.");
        issueController.resolveIssue(loginIssue.getId(), "tester1", "Verified on test server.");
        issueController.closeIssue(loginIssue.getId(), "PL1", "Closed after verification.");

        model.Issue searchIssue = issueController.createIssue(project.getId(), "Search filter ignores assignee",
                "Assignee filter returns all issues.", "tester2", model.Priority.MAJOR);
        issueController.assignIssue(searchIssue.getId(), "dev2", "PL1", "Please inspect search filtering.");
        issueController.markFixed(searchIssue.getId(), "dev2", "Fixed assignee condition.");
        issueController.resolveIssue(searchIssue.getId(), "tester2", "Verified filter result.");

        issueController.createIssue(project.getId(), "Issue detail comment order is reversed",
                "Newest comment is shown before older comments.", "tester1", model.Priority.MINOR);
    }

    public static class Controllers {
        private final controller.IssueController issueController;
        private final ProjectController projectController;
        private final UserController userController;

        private Controllers(
                controller.IssueController issueController,
                ProjectController projectController,
                UserController userController) {
            this.issueController = issueController;
            this.projectController = projectController;
            this.userController = userController;
        }

        public controller.IssueController issueController() {
            return issueController;
        }

        public ProjectController projectController() {
            return projectController;
        }

        public UserController userController() {
            return userController;
        }
    }
}
