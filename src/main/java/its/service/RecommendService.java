package its.service;

import java.util.List;

public interface RecommendService {

    //추천한다. 반환값은 이름
    List<String> recommendUser(String issueId, int topN);

    //추천도 계산을 수동으로 실행시킨다.
    void cal(String projectId);
}
