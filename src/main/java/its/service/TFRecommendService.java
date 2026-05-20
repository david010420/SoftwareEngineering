package its.service;

import its.model.Issue;
import its.model.IssueStatus;
import its.repository.IssueRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TFRecommendService implements RecommendService {

    private final IssueRepository issueRepository;

    //idfTable에서 계산해 놓은 값들
    private final Map<String, List<IndexEntry>> index = new ConcurrentHashMap<>();

    /** 프로젝트별 단어 점수 저장 projectId, 단어, 점수 순서*/
    private final Map<String, Map<String, Double>> idfTable = new ConcurrentHashMap<>();

    public TFRecommendService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    @Override
    public List<String> recommendUser(String issueId, int topN) {
        return null;
    }

    @Override
    public void cal(String projectId) {
        List<Issue> issueList = issueRepository.findByProjectId(projectId).stream()
                .filter(i -> i.getStatus() == IssueStatus.RESOLVED || i.getStatus() == IssueStatus.CLOSED)
                .filter(i -> i.getFixer() != null).toList();

        //만약 계산할 이슈가 존재하지 않는다면 바로 끝낸다
        if (issueList.isEmpty()) {
            index.remove(projectId);
            idfTable.remove(projectId);
            return;
        }

        //각 이슈를 토큰으로 분리
        List<List<String>> tokenized = issueList.stream()
                .map(i -> tokenize(i.getTitle() + " " + i.getDescription())).toList();

        // IDF 계산
        Map<String, Double> idf = computeIdf(tokenized);
        idfTable.put(projectId, idf);

        //각 문서의 TF-IDF 벡터 생성 및 색인 저장
        List<IndexEntry> entries = new ArrayList<>();
        for (int i = 0; i < issueList.size(); i++) {
            Map<String, Double> tfidf = computeTfIdf(tokenized.get(i), idf);
            entries.add(new IndexEntry(issueList.get(i).getFixer(), tfidf));
        }
        index.put(projectId, entries);
    }

    private static final class IndexEntry {
        final String              fixer;
        final Map<String, Double> tfidfVector;

        IndexEntry(String fixer, Map<String, Double> tfidfVector) {
            this.fixer       = fixer;
            this.tfidfVector = tfidfVector;
        }

        public String getFixer() {return fixer;}
    }

    //들어온 텍스트를 분리한다.
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(t -> t.length() > 1)
                .collect(Collectors.toList());
    }

    //
    private Map<String, Double> computeIdf(List<List<String>> corpus) {
        int N = corpus.size();
        Map<String, Integer> df = new HashMap<>();

        for (List<String> doc : corpus) {
            Set<String> uniqueTerms = new HashSet<>(doc);
            for (String term : uniqueTerms) {
                if (df.containsKey(term)) {
                    df.put(term, df.get(term) + 1);
                } else {
                    df.put(term, 1);
                }
            }
        }

        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : df.entrySet()) {
            String term = entry.getKey();
            int count = entry.getValue();
            idf.put(term, Math.log((N + 1.0) / (count + 1.0)) + 1.0);
        }

        return idf;
    }

    //단어의 빈도를 게산
    private Map<String, Double> computeTf(List<String> tokens) {
        //비었으면 바로 반환
        if (tokens.isEmpty()) return Collections.emptyMap();

        Map<String, Long> freq = new HashMap<>();
        for (String t : tokens) {
            if (freq.containsKey(t)) {
                freq.put(t, freq.get(t) + 1L);  // t가 이미 있으면 기존 값 + 1
            } else {
                freq.put(t, 1L);                 // t가 없으면 1로 초기화
            }
        }

        //각 토큰별로 가중치 계산
        double total = tokens.size();
        Map<String, Double> tf = new HashMap<>();
        for (Map.Entry<String, Long> entry : freq.entrySet()) {
            String term = entry.getKey();
            Long count = entry.getValue();
            tf.put(term, count / total);
        }
        return tf;
    }

    private Map<String, Double> computeTfIdf(List<String> tokens,
                                             Map<String, Double> idf) {
        Map<String, Double> tf = computeTf(tokens);
        Map<String, Double> tfidf = new HashMap<>();
        tf.forEach((term, tfScore) -> {
            double idfScore = idf.getOrDefault(term, Math.log(2.0) + 1.0); // 미등록 term
            tfidf.put(term, tfScore * idfScore);
        });
        return tfidf;
    }
}
