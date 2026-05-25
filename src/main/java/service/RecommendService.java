package service;

import java.util.List;

public interface RecommendService {
    List<String> recommendUser(long issueId, int topN);

    void calculate(long projectId);
}
