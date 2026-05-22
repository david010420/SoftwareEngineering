package its.service;

import its.repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TFRecommendServiceTest {

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private TFRecommendService service;

    @Test
    void cal_이슈가존재하지않으면_종료() {

    }

    @Test
    void cal_NEW상태만존재한다면_종료() {

    }

    @Test
    void cal_fixer없는이슈만있다면_종료() {

    }
}
