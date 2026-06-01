package service;

import model.Issue;
import model.IssueComment;
import model.IssueNotFoundException;
import model.IssueStatus;
import model.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.IssueRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock
    IssueRepository issueRepository;

    @InjectMocks
    IssueService issueService;

    private Issue issue;
    private long nextCommentId = 1L;

    @BeforeEach
    void setUp() {
        nextCommentId = 1L;
        issue = new Issue(1L, 1L, "title", "desc", "tester1", LocalDateTime.now(),
                null, null, Priority.MAJOR, IssueStatus.NEW, new ArrayList<>());

        lenient().when(issueRepository.findById(1L)).thenReturn(Optional.of(issue));
        lenient().when(issueRepository.findCommentsByIssueId(1L)).thenAnswer(invocation ->
                new ArrayList<>(issue.getComments()));
        lenient().doAnswer(invocation -> {
            issue = invocation.getArgument(0);
            when(issueRepository.findById(issue.getId())).thenReturn(Optional.of(issue));
            return null;
        }).when(issueRepository).update(any());
        lenient().when(issueRepository.addComment(eq(1L), any())).thenAnswer(invocation -> {
            IssueComment comment = invocation.getArgument(1);
            comment.setId(nextCommentId++);
            return comment;
        });
    }

    @Test
    void addComment_성공_이슈에_코멘트가_합성된다() {
        IssueComment comment = issueService.addComment(issue.getId(), "tester1", "first");

        assertEquals(1, issue.getComments().size());
        assertSame(comment, issue.getComments().get(0));
        assertEquals("first", comment.getBody());
        verify(issueRepository).addComment(eq(issue.getId()), eq(comment));
    }

    @Test
    void getIssue_없는이슈면_IssueNotFoundException() {
        when(issueRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class, () -> issueService.getIssue(123L));
    }

    @Test
    void 상태전이_시나리오_NEW_ASSIGNED_FIXED_RESOLVED_CLOSED() {
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
        issueService.assignIssue(issue.getId(), "dev1", "PL1", "assign");

        assertThrows(IllegalStateException.class, () -> issueService.markFixed(issue.getId(), "dev2", "fixed"));
    }

    @Test
    void addComment_여러번추가하면_누적된다() {
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
