package service;

import model.Issue;
import model.IssueStatus;
import repository.IssueRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TFRecommendService implements RecommendService {

    private final IssueRepository issueRepository;

    //idfTable에서 계산해 놓은 값들
    private final Map<Long, List<IndexEntry>> index = new ConcurrentHashMap<>();

    /** 프로젝트별 단어 점수 저장 projectId, 단어, 점수 순서*/
    private final Map<Long, Map<String, Double>> idfTable = new ConcurrentHashMap<>();

    public TFRecommendService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    @Override
    public List<String> recommendUser(Long issueId, int topN) {
        Issue target = issueRepository.findById(issueId).orElseThrow(() -> new RuntimeException(String.valueOf(issueId)));
        Long projectId = target.getProjectId();
        if (!index.containsKey(projectId)) {
            cal(projectId);
        }

        List<IndexEntry> entries = index.getOrDefault(projectId, List.of());
        if (entries.isEmpty()) {
            return List.of();   // 학습할 해결 이슈가 없음
        }

        // 1) 타깃 이슈의 TF-IDF 벡터 계산
        Map<String, Double> idf = idfTable.getOrDefault(projectId, Map.of());
        List<String> targetTokens =
                tokenize(target.getTitle() + " " + target.getDescription());
        Map<String, Double> targetVector = computeTfIdf(targetTokens, idf);

        if (targetVector.isEmpty()) {
            return List.of();
        }

        // 2) 각 기존 이슈와 코사인 유사도 계산 → fixer별 최고 점수 집계
        Map<String, Double> scoreByFixer = new HashMap<>();
        for (IndexEntry entry : entries) {
            double sim = cosineSimilarity(targetVector, entry.tfidfVector);
            if (sim <= 0) continue;
            scoreByFixer.merge(entry.fixer, sim, Math::max);
        }

        // 3) 점수 내림차순 정렬 후 상위 topN fixer 반환
        return scoreByFixer.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // 코사인 유사도
    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        // 예외 처리
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        // 더 작은 맵을 순회해서 내적 계산
        Map<String, Double> smaller = a.size() <= b.size() ? a : b;
        Map<String, Double> larger  = a.size() <= b.size() ? b : a;

        double dot = 0.0;
        for (Map.Entry<String, Double> e : smaller.entrySet()) {
            Double other = larger.get(e.getKey());
            if (other != null) dot += e.getValue() * other;
        }

        double normA = norm(a);
        double normB = norm(b);
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (normA * normB);
    }

    private double norm(Map<String, Double> vector) {
        double sum = 0.0;
        for (double v : vector.values()) sum += v * v;
        return Math.sqrt(sum);
    }

    @Override
    public void cal(Long projectId) {
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

    //들어온 텍스트를 분리한다.
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(t -> t.length() > 1)
                .collect(Collectors.toList());
    }

    private Map<String, Double> computeIdf(List<List<String>> corpus) {
        int N = corpus.size();
        Map<String, Integer> df = new HashMap<>();

        //이슈에 들어있는 단어들의 빈도를 계산한다.
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

        //단어의 IDF값을 계산한다. 자주 사용되는 단어는 판별력을 줄인다.
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
        double total = tokens.size();       //문서 전체에서 어느 빈도로 나왔는가
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

    private static final class IndexEntry {
        final String fixer;
        final Map<String, Double> tfidfVector;

        IndexEntry(String fixer, Map<String, Double> tfidfVector) {
            this.fixer       = fixer;
            this.tfidfVector = tfidfVector;
        }
    }

}

