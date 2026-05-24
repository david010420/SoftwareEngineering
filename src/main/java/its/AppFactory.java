package its;

import its.controller.IssueController;
import its.repository.SqliteIssueRepository;
import its.service.IssueService;

public final class AppFactory {
    private AppFactory() {
    }

//    public static IssueController createController() {
//        IssueController controller = new IssueController(
//                new IssueService(new SqliteIssueRepository("data/its.db")));
//        controller.seedDemoData();
//        return controller;
//    }
}
