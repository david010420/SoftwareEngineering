package service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import model.IssueStatus;
import model.Priority;

public class IssueStatistics {
    private final Map<LocalDate, Long> dailyCounts;
    private final Map<YearMonth, Long> monthlyCounts;
    private final Map<IssueStatus, Long> statusCounts;
    private final Map<Priority, Long> priorityCounts;
    private final Map<String, Long> assigneeCounts;

    public IssueStatistics(Map<LocalDate, Long> dailyCounts, Map<YearMonth, Long> monthlyCounts) {
        this.dailyCounts = Objects.requireNonNull(dailyCounts, "dailyCounts");
        this.monthlyCounts = Objects.requireNonNull(monthlyCounts, "monthlyCounts");
        this.statusCounts = Collections.emptyMap();
        this.priorityCounts = Collections.emptyMap();
        this.assigneeCounts = Collections.emptyMap();
    }

    public IssueStatistics(
            Map<LocalDate, Long> dailyCounts,
            Map<YearMonth, Long> monthlyCounts,
            Map<IssueStatus, Long> statusCounts,
            Map<Priority, Long> priorityCounts,
            Map<String, Long> assigneeCounts) {
        this.dailyCounts = Objects.requireNonNull(dailyCounts, "dailyCounts");
        this.monthlyCounts = Objects.requireNonNull(monthlyCounts, "monthlyCounts");
        this.statusCounts = unmodifiableCopy(statusCounts, "statusCounts");
        this.priorityCounts = unmodifiableCopy(priorityCounts, "priorityCounts");
        this.assigneeCounts = unmodifiableCopy(assigneeCounts, "assigneeCounts");
    }

    public Map<LocalDate, Long> getDailyCounts() {
        return dailyCounts;
    }

    public Map<YearMonth, Long> getMonthlyCounts() {
        return monthlyCounts;
    }

    public Map<IssueStatus, Long> getStatusCounts() {
        return statusCounts;
    }

    public Map<Priority, Long> getPriorityCounts() {
        return priorityCounts;
    }

    public Map<String, Long> getAssigneeCounts() {
        return assigneeCounts;
    }

    private static <K, V> Map<K, V> unmodifiableCopy(Map<K, V> source, String name) {
        if (source == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
