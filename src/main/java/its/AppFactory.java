package its;

import its.controller.IssueController;
import its.repository.FileIssueRepository;
import its.service.IssueService;

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
}
