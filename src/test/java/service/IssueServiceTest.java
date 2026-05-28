package service;

import model.Issue;
import model.IssueComment;
import model.IssueNotFoundException;
import model.IssueStatus;
import model.Priority;
import model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryIssueRepository;
import repository.InMemoryProjectRepository;

import static org.junit.jupiter.api.Assertions.*;

class IssueServiceTest {

    InMemoryProjectRepository projectRepository;
    InMemoryIssueRepository issueRepository;
    ProjectService projectService;
    IssueService issueService;

    @BeforeEach
    void setUp() {
        projectRepository = new InMemoryProjectRepository();
        issueRepository = new InMemoryIssueRepository();
        projectService = new ProjectService(projectRepository, issueRepository);
        issueService = new IssueService(issueRepository, projectRepository);
    }

    @Test
    void reportIssue_성공_NEW_기본우선순위_MAJOR() {
        Project project = projectService.createProject("project1");

        Issue issue = issueService.reportIssue(project.getId(), "title", "desc", "tester1", null);

        assertTrue(issue.getId() > 0);
        assertEquals(project.getId(), issue.getProjectId());
        assertEquals("tester1", issue.getReporterUsername());
        assertEquals(IssueStatus.NEW, issue.getStatus());
        assertEquals(Priority.MAJOR, issue.getPriority());
        assertNotNull(issue.getReportedAt());
    }

    @Test
    void reportIssue_없는프로젝트면_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> issueService.reportIssue(999L, "t", "d", "tester1", Priority.MAJOR));
    }

    @Test
    void getIssue_없는이슈면_IssueNotFoundException() {
        assertThrows(IssueNotFoundException.class, () -> issueService.getIssue(123L));
    }

    @Test
    void 상태전이_시나리오_NEW_ASSIGNED_FIXED_RESOLVED_CLOSED() {
        Project project = projectService.createProject("project1");
        Issue issue = issueService.reportIssue(project.getId(), "title", "desc", "tester1", Priority.MAJOR);

        issue = issueService.assignIssue(issue.getId(), "dev1", "PL1", "assign");
        assertEquals(IssueStatus.ASSIGNED, issue.getStatus());
        assertEquals("dev1", issue.getAssigneeUsername());

        issue = issueService.markFixed(issue.getId(), "dev1", "fixed");
        assertEquals(IssueStatus.FIXED, issue.getStatus());
        assertEquals("dev1", issue.getFixerUsername());

        issue = issueService.resolveIssue(issue.getId(), "tester1", "resolved");
        assertEquals(IssueStatus.RESOLVED, issue.getStatus());

        issue = issueService.closeIssue(issue.getId(), "PL1", "closed");
        assertEquals(IssueStatus.CLOSED, issue.getStatus());
    }

    @Test
    void markFixed_다른개발자면_예외() {
        Project project = projectService.createProject("project1");
        Issue issue = issueService.reportIssue(project.getId(), "title", "desc", "tester1", Priority.MAJOR);
        issueService.assignIssue(issue.getId(), "dev1", "PL1", "assign");

        assertThrows(IllegalStateException.class, () -> issueService.markFixed(issue.getId(), "dev2", "fixed"));
    }

    @Test
    void addComment_여러번추가하면_누적된다() {
        Project project = projectService.createProject("project1");
        Issue issue = issueService.reportIssue(project.getId(), "title", "desc", "tester1", Priority.MAJOR);

        IssueComment c1 = issueService.addComment(issue.getId(), "tester1", "first");
        IssueComment c2 = issueService.addComment(issue.getId(), "pl1", "second");

        Issue loaded = issueService.getIssue(issue.getId());
        assertEquals(2, loaded.getComments().size());
        assertEquals("first", loaded.getComments().get(0).getBody());
        assertEquals("second", loaded.getComments().get(1).getBody());
        assertTrue(c1.getId() > 0);
        assertTrue(c2.getId() > 0);
    }
}

