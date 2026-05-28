package repository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.Issue;
import model.IssueComment;
import model.IssueStatus;
import model.Priority;

public interface IssueRepository {
    void initialize();

    Issue save(Issue issue);

    void update(Issue issue);

    Optional<Issue> findById(long id);



    List<Issue> findAll();

    List<Issue> findByProjectId(long projectId);

    List<Issue> findByReporterUsername(String reporterUsername);

    List<Issue> findByAssigneeUsername(String assigneeUsername);

    List<Issue> findByStatus(IssueStatus status);

    List<Issue> findByPriority(Priority priority);

    List<Issue> searchByKeyword(String keyword);

    IssueComment addComment(long issueId, IssueComment comment);

    List<IssueComment> findCommentsByIssueId(long issueId);

    Map<LocalDate, Long> countReportedByDay();

    Map<YearMonth, Long> countReportedByMonth();
}
