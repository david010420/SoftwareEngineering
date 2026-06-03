package service;

import model.Issue;
import model.IssueComment;
import model.IssueStatus;
import model.Priority;
import model.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.IssueRepository;
import repository.ProjectRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @Mock
    IssueRepository issueRepository;

    @InjectMocks
    ProjectService projectService;

    private final Map<Long, Project> projects = new LinkedHashMap<>();
    private final Map<Long, Issue> issues = new LinkedHashMap<>();
    private long nextProjectId = 1L;
    private long nextIssueId = 1L;
    private long nextCommentId = 1L;

    @BeforeEach
    void setUp() {
        projects.clear();
        issues.clear();
        nextProjectId = 1L;
        nextIssueId = 1L;
        nextCommentId = 1L;

        lenient().when(projectRepository.findAll()).thenAnswer(invocation -> List.copyOf(projects.values()));
        lenient().when(projectRepository.save(any())).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            if (project.getId() == 0L) {
                project.setId(nextProjectId++);
            }
            projects.put(project.getId(), project);
            return project;
        });
        lenient().when(projectRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(projects.get(invocation.getArgument(0))));
        lenient().when(projectRepository.exists(anyLong())).thenAnswer(invocation ->
                projects.containsKey(invocation.getArgument(0)));
        lenient().doAnswer(invocation -> {
            projects.remove(invocation.getArgument(0));
            return null;
        }).when(projectRepository).delete(anyLong());

        lenient().when(issueRepository.save(any())).thenAnswer(invocation -> {
            Issue issue = invocation.getArgument(0);
            if (issue.getId() == 0L) {
                issue.setId(nextIssueId++);
            }
            issues.put(issue.getId(), issue);
            return issue;
        });
        lenient().when(issueRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(issues.get(invocation.getArgument(0))));
        lenient().when(issueRepository.findByProjectId(anyLong())).thenAnswer(invocation -> {
            long projectId = invocation.getArgument(0);
            return issues.values().stream()
                    .filter(issue -> issue.getProjectId() == projectId)
                    .toList();
        });
        lenient().doAnswer(invocation -> {
            Issue issue = invocation.getArgument(0);
            issues.put(issue.getId(), issue);
            return null;
        }).when(issueRepository).update(any());
        lenient().when(issueRepository.addComment(anyLong(), any())).thenAnswer(invocation -> {
            IssueComment comment = invocation.getArgument(1);
            comment.setId(nextCommentId++);
            return comment;
        });
        lenient().when(issueRepository.findCommentsByIssueId(anyLong())).thenAnswer(invocation -> {
            Issue issue = issues.get(invocation.getArgument(0));
            return issue == null ? new ArrayList<>() : new ArrayList<>(issue.getComments());
        });
        lenient().when(issueRepository.deleteByProjectId(anyLong())).thenAnswer(invocation -> {
            long projectId = invocation.getArgument(0);
            List<Long> ids = issues.values().stream()
                    .filter(issue -> issue.getProjectId() == projectId)
                    .map(Issue::getId)
                    .toList();
            ids.forEach(issues::remove);
            return ids.size();
        });
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
    void addIssue_성공_프로젝트에_이슈가_합성된다() {
        Project project = projectService.createProject("project1");

        Issue issue = projectService.addIssue(project.getId(), "title", "desc", "tester1", null);

        assertTrue(issue.getId() > 0);
        assertEquals(1, project.getIssues().size());
        assertSame(issue, project.getIssues().get(0));
        assertEquals(IssueStatus.NEW, issue.getStatus());
        assertEquals(Priority.MAJOR, issue.getPriority());
        verify(issueRepository).save(issue);
    }

    @Test
    void addIssue_없는프로젝트면_예외() {
        assertThrows(IllegalArgumentException.class,
                () -> projectService.addIssue(999L, "t", "d", "tester1", Priority.MAJOR));
    }

    @Test
    void deleteProject_프로젝트와_해당이슈들이_같이삭제된다() {
        Project project1 = projectService.createProject("project1");
        Project project2 = projectService.createProject("project2");
        IssueService issueService = new IssueService(issueRepository);

        Issue p1Issue = projectService.addIssue(project1.getId(), "t1", "d1", "tester1", Priority.MAJOR);
        issueService.addComment(p1Issue.getId(), "tester1", "c1");
        issueService.assignIssue(p1Issue.getId(), "dev1", "PL1", "assign");
        issueService.markFixed(p1Issue.getId(), "dev1", "fixed");
        issueService.resolveIssue(p1Issue.getId(), "tester1", "resolved");
        issueService.closeIssue(p1Issue.getId(), "PL1", "closed");

        Issue p2Issue = projectService.addIssue(project2.getId(), "t2", "d2", "tester1", Priority.MAJOR);
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
        verify(issueRepository).deleteByProjectId(project1.getId());
        verify(projectRepository).delete(project1.getId());
    }

    @Test
    void deleteProject_없는프로젝트면_예외() {
        assertThrows(IllegalArgumentException.class, () -> projectService.deleteProject(999L));
    }
}
