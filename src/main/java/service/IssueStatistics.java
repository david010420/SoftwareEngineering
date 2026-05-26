package service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Objects;

public class IssueStatistics {
    private final Map<LocalDate, Long> dailyCounts;
    private final Map<YearMonth, Long> monthlyCounts;

    public IssueStatistics(Map<LocalDate, Long> dailyCounts, Map<YearMonth, Long> monthlyCounts) {
        this.dailyCounts = Objects.requireNonNull(dailyCounts, "dailyCounts");
        this.monthlyCounts = Objects.requireNonNull(monthlyCounts, "monthlyCounts");
    }

    public Map<LocalDate, Long> getDailyCounts() {
        return dailyCounts;
    }

    public Map<YearMonth, Long> getMonthlyCounts() {
        return monthlyCounts;
    }
}
