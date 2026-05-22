package its.service;

import its.model.Comment;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Project;
import its.model.Role;
import its.model.UserAccount;
import its.repository.IssueRepository;
import its.repository.IssueStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IssueService {
    private static final String DEMO_PASSWORD = "1234";

    private final IssueRepository repository;

    public IssueService(IssueRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<Project> listProjects() {
        return List.copyOf(repository.load().getProjects());
    }

    public List<UserAccount> listUsers() {
        return List.copyOf(repository.load().getUsers());
    }

    public UserAccount login(String username, String password) {
        UserAccount user = requireUser(repository.load(), username);
        if (!user.checkPassword(password)) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        return user;
    }

    public List<Issue> listIssues() {
        return repository.load().getIssues().stream()
                .sorted(Comparator.comparing(Issue::getId))
                .collect(Collectors.toList());
    }

    public Project addProject(String name) {
        IssueStore store = repository.load();
        store.getProjects().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name.trim()))
                .findAny()
                .ifPresent(p -> {
                    throw new IllegalArgumentException("project already exists: " + name);
                });
        Project project = new Project(name);
        store.getProjects().add(project);
        repository.save(store);
        return project;
    }

    public UserAccount addUser(String username, Role role) {
        IssueStore store = repository.load();
        store.getUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findAny()
                .ifPresent(u -> {
                    throw new IllegalArgumentException("user already exists: " + username);
                });
        UserAccount user = new UserAccount(username, DEMO_PASSWORD, role);
        store.getUsers().add(user);
        repository.save(store);
        return user;
    }

    public Issue createIssue(String projectName, String title, String description, String reporter, Priority priority) {
        IssueStore store = repository.load();
        requireProject(store, projectName);
        requireUser(store, reporter);
        Issue issue = new Issue(store.nextIssueId(), projectName, title, description, reporter, LocalDateTime.now(), priority);
        store.getIssues().add(issue);
        repository.save(store);
        return issue;
    }

    public Issue addComment(long issueId, String author, String message) {
        IssueStore store = repository.load();
        requireUser(store, author);
        Issue issue = findIssue(store, issueId);
        issue.addComment(new Comment(author, message, LocalDateTime.now()));
        repository.save(store);
        return issue;
    }

    public Issue assignIssue(long issueId, String assignee, String actor, String comment) {
        IssueStore store = repository.load();
        requireRole(store, actor, Role.PL);
        requireRole(store, assignee, Role.DEV);
        Issue issue = findIssue(store, issueId);
        issue.assignTo(assignee);
        if (comment != null && !comment.isBlank()) {
            issue.addComment(new Comment(actor, comment, LocalDateTime.now()));
        }
        repository.save(store);
        return issue;
    }

    public Issue markFixed(long issueId, String fixer, String comment) {
        IssueStore store = repository.load();
        requireRole(store, fixer, Role.DEV);
        Issue issue = findIssue(store, issueId);
        issue.markFixed(fixer);
        if (comment != null && !comment.isBlank()) {
            issue.addComment(new Comment(fixer, comment, LocalDateTime.now()));
        }
        repository.save(store);
        return issue;
    }

    public Issue changeStatus(long issueId, IssueStatus status, String actor, String comment) {
        IssueStore store = repository.load();
        requireUser(store, actor);
        Issue issue = findIssue(store, issueId);
        if (status == IssueStatus.FIXED) {
            issue.markFixed(actor);
        } else {
            issue.changeStatus(status);
        }
        if (comment != null && !comment.isBlank()) {
            issue.addComment(new Comment(actor, comment, LocalDateTime.now()));
        }
        repository.save(store);
        return issue;
    }

    public List<Issue> search(IssueSearchCriteria criteria) {
        IssueSearchCriteria c = criteria == null ? new IssueSearchCriteria() : criteria;
        List<Predicate<Issue>> predicates = new ArrayList<>();
        if (c.getProjectName() != null) {
            predicates.add(i -> i.getProjectName().equalsIgnoreCase(c.getProjectName()));
        }
        if (c.getReporter() != null) {
            predicates.add(i -> i.getReporter().equalsIgnoreCase(c.getReporter()));
        }
        if (c.getAssignee() != null) {
            predicates.add(i -> c.getAssignee().equalsIgnoreCase(i.getAssignee()));
        }
        if (c.getStatus() != null) {
            predicates.add(i -> i.getStatus() == c.getStatus());
        }
        if (c.getQuery() != null) {
            predicates.add(i -> i.containsText(c.getQuery()));
        }
        return listIssues().stream()
                .filter(issue -> predicates.stream().allMatch(p -> p.test(issue)))
                .collect(Collectors.toList());
    }

    public IssueStatistics statistics() {
        Map<LocalDate, Long> daily = listIssues().stream()
                .collect(Collectors.groupingBy(i -> i.getReportedDate().toLocalDate(), LinkedHashMap::new, Collectors.counting()));
        Map<YearMonth, Long> monthly = listIssues().stream()
                .collect(Collectors.groupingBy(i -> YearMonth.from(i.getReportedDate()), LinkedHashMap::new, Collectors.counting()));
        return new IssueStatistics(daily, monthly);
    }

    public List<String> recommendAssignees(long issueId) {
        IssueStore store = repository.load();
        Issue target = findIssue(store, issueId);
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Issue issue : store.getIssues()) {
            if (issue.getFixer() == null) {
                continue;
            }
            if (issue.getStatus() != IssueStatus.RESOLVED && issue.getStatus() != IssueStatus.CLOSED) {
                continue;
            }
            int score = similarity(target, issue);
            if (score > 0) {
                scores.merge(issue.getFixer(), score, Integer::sum);
            }
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void seedDemoData() {
        IssueStore store = repository.load();
        if (!store.getProjects().isEmpty() || !store.getUsers().isEmpty() || !store.getIssues().isEmpty()) {
            return;
        }
        store.getProjects().add(new Project("project1"));
        store.getUsers().add(new UserAccount("admin", DEMO_PASSWORD, Role.ADMIN));
        store.getUsers().add(new UserAccount("PL1", DEMO_PASSWORD, Role.PL));
        store.getUsers().add(new UserAccount("PL2", DEMO_PASSWORD, Role.PL));
        for (int i = 1; i <= 10; i++) {
            store.getUsers().add(new UserAccount("dev" + i, DEMO_PASSWORD, Role.DEV));
        }
        for (int i = 1; i <= 5; i++) {
            store.getUsers().add(new UserAccount("tester" + i, DEMO_PASSWORD, Role.TESTER));
        }
        Issue login = new Issue(store.nextIssueId(), "project1", "Login fails after password reset",
                "Users cannot sign in after resetting a password.", "tester1", LocalDateTime.now().minusDays(3), Priority.CRITICAL);
        login.assignTo("dev1");
        login.addComment(new Comment("PL1", "Please check auth token refresh.", LocalDateTime.now().minusDays(3)));
        login.markFixed("dev1");
        login.changeStatus(IssueStatus.CLOSED);
        login.addComment(new Comment("tester1", "Verified on test server.", LocalDateTime.now().minusDays(2)));
        Issue search = new Issue(store.nextIssueId(), "project1", "Search filter ignores assignee",
                "Assignee filter returns all issues.", "tester2", LocalDateTime.now().minusDays(2), Priority.MAJOR);
        search.assignTo("dev2");
        search.markFixed("dev2");
        search.changeStatus(IssueStatus.RESOLVED);
        Issue ui = new Issue(store.nextIssueId(), "project1", "Issue detail comment order is reversed",
                "Newest comment is shown before older comments.", "tester1", LocalDateTime.now().minusDays(1), Priority.MINOR);
        store.getIssues().add(login);
        store.getIssues().add(search);
        store.getIssues().add(ui);
        repository.save(store);
    }

    private static int similarity(Issue a, Issue b) {
        List<String> aTerms = terms(a.getTitle() + " " + a.getDescription());
        List<String> bTerms = terms(b.getTitle() + " " + b.getDescription());
        int score = 0;
        for (String term : aTerms) {
            if (bTerms.contains(term)) {
                score++;
            }
        }
        return score;
    }

    private static List<String> terms(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9가-힣]+")).stream()
                .filter(s -> s.length() >= 3)
                .distinct()
                .collect(Collectors.toList());
    }

    private static Issue findIssue(IssueStore store, long issueId) {
        return store.getIssues().stream()
                .filter(i -> i.getId() == issueId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("issue not found: " + issueId));
    }

    private static void requireProject(IssueStore store, String projectName) {
        store.getProjects().stream()
                .filter(p -> p.getName().equalsIgnoreCase(projectName.trim()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("project not found: " + projectName));
    }

    private static UserAccount requireUser(IssueStore store, String username) {
        return store.getUsers().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + username));
    }

    private static void requireRole(IssueStore store, String username, Role role) {
        UserAccount user = requireUser(store, username);
        if (user.getRole() != role) {
            throw new IllegalArgumentException(username + " must be " + role);
        }
    }
}
