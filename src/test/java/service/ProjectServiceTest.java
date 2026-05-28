package service;

import model.Issue;
import model.IssueStatus;
import model.Priority;
import model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.InMemoryIssueRepository;
import repository.InMemoryProjectRepository;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest {

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
    void createProject_성공() {
        Project project = projectService.createProject("project1");

        assertTrue(project.getId() > 0);
        assertEquals("project1", project.getName());
    }

    @Test
    void createProject_이름중복이면_예외() {
        projectService.createProject("project1");

        assertThrows(IllegalArgumentException.class, () -> projectService.createProject("PROJECT1"));
    }

    @Test
    void deleteProject_프로젝트와_해당이슈들이_같이삭제된다() {
        Project project1 = projectService.createProject("project1");
        Project project2 = projectService.createProject("project2");

        Issue p1Issue = issueService.reportIssue(project1.getId(), "t1", "d1", "tester1", Priority.MAJOR);
        issueService.addComment(p1Issue.getId(), "tester1", "c1");
        issueService.addComment(p1Issue.getId(), "pl1", "c2");
        issueService.assignIssue(p1Issue.getId(), "dev1", "PL1", "assign");
        issueService.markFixed(p1Issue.getId(), "dev1", "fixed");
        issueService.resolveIssue(p1Issue.getId(), "tester1", "resolved");
        issueService.closeIssue(p1Issue.getId(), "PL1", "closed");

        Issue p2Issue = issueService.reportIssue(project2.getId(), "t2", "d2", "tester1", Priority.MAJOR);
        assertEquals(1, issueRepository.findByProjectId(project1.getId()).size());
        assertEquals(1, issueRepository.findByProjectId(project2.getId()).size());

        projectService.deleteProject(project1.getId());

        assertFalse(projectRepository.exists(project1.getId()));
        assertTrue(projectRepository.exists(project2.getId()));
        assertEquals(0, issueRepository.findByProjectId(project1.getId()).size());
        assertEquals(1, issueRepository.findByProjectId(project2.getId()).size());
        assertFalse(issueRepository.findById(p1Issue.getId()).isPresent());
        assertTrue(issueRepository.findById(p2Issue.getId()).isPresent());
        assertEquals(0, issueRepository.findCommentsByIssueId(p1Issue.getId()).size());
    }

    @Test
    void deleteProject_없는프로젝트면_예외() {
        assertThrows(IllegalArgumentException.class, () -> projectService.deleteProject(999L));
    }
}

