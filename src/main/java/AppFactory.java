import controller.IssueController;
import repository.SqliteIssueRepository;
import service.IssueService;

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
