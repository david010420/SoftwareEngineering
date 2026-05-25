package service;

import model.Issue;
import model.IssueStatus;
import repository.IssueRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TFRecommendService implements RecommendService {
    private final IssueRepository issueRepository;
    private final Map<Long, List<IndexEntry>> index = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Double>> idfTable = new ConcurrentHashMap<>();

    public TFRecommendService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    @Override
    public List<String> recommendUser(long issueId, int topN) {
        Issue target = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("issue not found: " + issueId));
        long projectId = target.getProjectId();
        if (!index.containsKey(projectId)) {
            calculate(projectId);
        }
        List<IndexEntry> entries = index.getOrDefault(projectId, Collections.emptyList());
        if (entries.isEmpty()) {
            return List.of();
        }

        Map<String, Double> targetVector = computeTfIdf(
                tokenize(target.getTitle() + " " + target.getDescription()),
                idfTable.getOrDefault(projectId, Collections.emptyMap()));

        Map<String, Double> scores = new LinkedHashMap<>();
        for (IndexEntry entry : entries) {
            double score = cosineSimilarity(targetVector, entry.tfidfVector);
            scores.merge(entry.fixer, score, Double::sum);
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(0, topN))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public void calculate(long projectId) {
        List<Issue> issueList = issueRepository.findByProjectId(projectId).stream()
                .filter(issue -> issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED)
                .filter(issue -> issue.getFixer() != null)
                .toList();

        if (issueList.isEmpty()) {
            index.remove(projectId);
            idfTable.remove(projectId);
            return;
        }

        List<List<String>> tokenized = issueList.stream()
                .map(issue -> tokenize(issue.getTitle() + " " + issue.getDescription()))
                .toList();
        Map<String, Double> idf = computeIdf(tokenized);
        idfTable.put(projectId, idf);

        List<IndexEntry> entries = new ArrayList<>();
        for (int i = 0; i < issueList.size(); i++) {
            entries.add(new IndexEntry(issueList.get(i).getFixer(), computeTfIdf(tokenized.get(i), idf)));
        }
        index.put(projectId, entries);
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(text.toLowerCase().split("[\\s\\p{Punct}]+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toList());
    }

    private Map<String, Double> computeIdf(List<List<String>> corpus) {
        int totalDocuments = corpus.size();
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> document : corpus) {
            Set<String> uniqueTerms = new HashSet<>(document);
            for (String term : uniqueTerms) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        Map<String, Double> idf = new HashMap<>();
        documentFrequency.forEach((term, count) ->
                idf.put(term, Math.log((totalDocuments + 1.0) / (count + 1.0)) + 1.0));
        return idf;
    }

    private Map<String, Double> computeTf(List<String> tokens) {
        if (tokens.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> frequency = new HashMap<>();
        for (String token : tokens) {
            frequency.merge(token, 1L, Long::sum);
        }
        double total = tokens.size();
        Map<String, Double> tf = new HashMap<>();
        frequency.forEach((term, count) -> tf.put(term, count / total));
        return tf;
    }

    private Map<String, Double> computeTfIdf(List<String> tokens, Map<String, Double> idf) {
        Map<String, Double> tf = computeTf(tokens);
        Map<String, Double> tfidf = new HashMap<>();
        tf.forEach((term, tfScore) -> tfidf.put(term, tfScore * idf.getOrDefault(term, Math.log(2.0) + 1.0)));
        return tfidf;
    }

    private double cosineSimilarity(Map<String, Double> left, Map<String, Double> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : left.entrySet()) {
            dot += entry.getValue() * right.getOrDefault(entry.getKey(), 0.0);
        }
        double leftNorm = norm(left);
        double rightNorm = norm(right);
        return leftNorm == 0.0 || rightNorm == 0.0 ? 0.0 : dot / (leftNorm * rightNorm);
    }

    private double norm(Map<String, Double> vector) {
        return Math.sqrt(vector.values().stream().mapToDouble(value -> value * value).sum());
    }

    private static final class IndexEntry {
        private final String fixer;
        private final Map<String, Double> tfidfVector;

        private IndexEntry(String fixer, Map<String, Double> tfidfVector) {
            this.fixer = fixer;
            this.tfidfVector = tfidfVector;
        }
    }
}
