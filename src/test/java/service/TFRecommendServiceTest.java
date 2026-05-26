package service;

import model.Issue;
import model.IssueStatus;
import model.Priority;
import repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TFRecommendServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private TFRecommendService service;

    @Test
    void cal_이슈가존재하지않으면_종료() {
        when(issueRepository.findByProjectId("proj1")).thenReturn(List.of());
        assertDoesNotThrow(() -> service.cal("proj1"));
    }

    @Test
    void cal_NEW상태만존재한다면_종료() {
        when(issueRepository.findByProjectId("proj1"))
                .thenReturn(List.of(newIssue(1L), newIssue(2L)));
        assertDoesNotThrow(() -> service.cal("proj1"));
    }

    @Test
    void cal_fixer없는이슈만있다면_종료() {
        Issue issue = newIssue(1L);
        issue.changeStatus(IssueStatus.RESOLVED);
        when(issueRepository.findByProjectId("proj1")).thenReturn(List.of(issue));
        assertDoesNotThrow(() -> service.cal("proj1"));
    }

    private Issue newIssue(long id) {
        return new Issue(id, "proj1", String.valueOf(id), "제목" + id, "설명" + id,
                "tester1", LocalDateTime.now(), Priority.MAJOR);
    }
}
