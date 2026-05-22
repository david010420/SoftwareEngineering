package its;

import its.controller.IssueController;
import its.controller.UserController;
import its.repository.FileIssueRepository;
import its.repository.FileUserRepository;
import its.service.IssueService;
import its.service.UserServiceImpl;

import java.nio.file.Path;

public final class AppFactory {
    private AppFactory() {
    }

    public static IssueController createController() {
        Path dataPath = Path.of("data", "issues.store");
        IssueController controller = new IssueController(new IssueService(new FileIssueRepository(dataPath)));
        controller.seedDemoData();
        return controller;
    }

    public static Controllers createControllers() {
        Path dataPath = Path.of("data", "issues.store");
        FileIssueRepository issueRepository = new FileIssueRepository(dataPath);
        IssueController issueController = new IssueController(new IssueService(issueRepository));
        issueController.seedDemoData();
        UserController userController = new UserController(new UserServiceImpl(new FileUserRepository(issueRepository)));
        return new Controllers(issueController, userController);
    }

    public static class Controllers {
        private final IssueController issueController;
        private final UserController userController;

        private Controllers(IssueController issueController, UserController userController) {
            this.issueController = issueController;
            this.userController = userController;
        }

        public IssueController issueController() {
            return issueController;
        }

        public UserController userController() {
            return userController;
        }
    }
}
