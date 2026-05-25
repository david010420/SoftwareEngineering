package its;

import its.repository.FileIssueRepository;

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

}
