package its.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

public class IssueStatistics {
    private final Map<LocalDate, Long> dailyCounts;
    private final Map<YearMonth, Long> monthlyCounts;

    public IssueStatistics(Map<LocalDate, Long> dailyCounts, Map<YearMonth, Long> monthlyCounts) {
        this.dailyCounts = dailyCounts;
        this.monthlyCounts = monthlyCounts;
    }

    public Map<LocalDate, Long> getDailyCounts() {
        return dailyCounts;
    }

    public Map<YearMonth, Long> getMonthlyCounts() {
        return monthlyCounts;
    }
}
