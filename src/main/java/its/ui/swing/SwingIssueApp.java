package its.ui.swing;

import its.AppFactory;
import its.controller.IssueController;
import its.model.Comment;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Role;
import its.service.IssueSearchCriteria;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SwingIssueApp extends JFrame {
    private final IssueController controller;
    private final DefaultListModel<Issue> issueListModel = new DefaultListModel<>();
    private final JList<Issue> issueList = new JList<>(issueListModel);
    private final JTextArea detailArea = new JTextArea();
    private final JTextField queryField = new JTextField();
    private final JTextField reporterField = new JTextField();
    private final JTextField assigneeField = new JTextField();
    private final JComboBox<String> statusBox = new JComboBox<>();

    public SwingIssueApp(IssueController controller) {
        super("Issue Management System - Swing");
        this.controller = controller;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        buildUi();
        refreshIssues(controller.issues());
    }

    private void buildUi() {
        JPanel filters = new JPanel(new GridLayout(2, 5, 6, 6));
        statusBox.addItem("");
        for (IssueStatus status : IssueStatus.values()) {
            statusBox.addItem(status.name());
        }
        filters.add(new JLabel("Query"));
        filters.add(new JLabel("Reporter"));
        filters.add(new JLabel("Assignee"));
        filters.add(new JLabel("Status"));
        filters.add(new JLabel(""));
        filters.add(queryField);
        filters.add(reporterField);
        filters.add(assigneeField);
        filters.add(statusBox);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> search());
        filters.add(searchButton);

        detailArea.setEditable(false);
        issueList.addListSelectionListener(e -> showSelectedIssue());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(issueList), new JScrollPane(detailArea));
        split.setDividerLocation(360);

        JPanel actions = new JPanel(new GridLayout(2, 5, 6, 6));
        addButton(actions, "Add User", this::addUser);
        addButton(actions, "New Issue", this::newIssue);
        addButton(actions, "Comment", this::addComment);
        addButton(actions, "Assign", this::assign);
        addButton(actions, "Fix", this::fix);
        addButton(actions, "Resolve", () -> changeStatus(IssueStatus.RESOLVED));
        addButton(actions, "Close", () -> changeStatus(IssueStatus.CLOSED));
        addButton(actions, "Reopen", () -> changeStatus(IssueStatus.REOPENED));
        addButton(actions, "Recommend", this::recommend);
        addButton(actions, "Stats", this::stats);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(filters, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> runSafely(action));
        panel.add(button);
    }

    private void search() {
        IssueStatus status = statusBox.getSelectedItem() == null || statusBox.getSelectedItem().toString().isBlank()
                ? null : IssueStatus.valueOf(statusBox.getSelectedItem().toString());
        refreshIssues(controller.search(new IssueSearchCriteria()
                .query(queryField.getText())
                .reporter(reporterField.getText())
                .assignee(assigneeField.getText())
                .status(status)));
    }

    private void addUser() {
        String username = input("Username");
        if (username == null) {
            return;
        }
        Role role = (Role) JOptionPane.showInputDialog(this, "Role", "Add User",
                JOptionPane.PLAIN_MESSAGE, null, Role.values(), Role.DEV);
        if (role != null) {
            controller.addUser(username, role);
            message("User added.");
        }
    }

    private void newIssue() {
        String title = input("Title");
        String description = input("Description");
        String reporter = input("Reporter");
        if (title != null && description != null && reporter != null) {
            controller.createIssue("project1", title, description, reporter, Priority.MAJOR);
            refreshIssues(controller.issues());
        }
    }

    private void addComment() {
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String author = input("Author");
        String message = input("Comment");
        if (author != null && message != null) {
            controller.addComment(issue.getId(), author, message);
            refreshIssues(controller.issues());
        }
    }

    private void assign() {
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String assignee = input("Assignee dev account");
        String actor = input("PL actor");
        String comment = input("Comment");
        if (assignee != null && actor != null) {
            controller.assignIssue(issue.getId(), assignee, actor, comment == null ? "" : comment);
            refreshIssues(controller.issues());
        }
    }

    private void fix() {
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String fixer = input("Fixer dev account");
        String comment = input("Comment");
        if (fixer != null) {
            controller.markFixed(issue.getId(), fixer, comment == null ? "" : comment);
            refreshIssues(controller.issues());
        }
    }

    private void changeStatus(IssueStatus status) {
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String actor = input("Actor");
        String comment = input("Comment");
        if (actor != null) {
            controller.changeStatus(issue.getId(), status, actor, comment == null ? "" : comment);
            refreshIssues(controller.issues());
        }
    }

    private void recommend() {
        Issue issue = selectedIssue();
        if (issue != null) {
            List<String> candidates = controller.recommendAssignees(issue.getId());
            message(candidates.isEmpty() ? "No candidate yet." : "Best candidate: " + String.join(", ", candidates));
        }
    }

    private void stats() {
        StringBuilder builder = new StringBuilder("Daily\n");
        controller.statistics().getDailyCounts().forEach((day, count) -> builder.append(day).append(": ").append(count).append('\n'));
        builder.append("\nMonthly\n");
        controller.statistics().getMonthlyCounts().forEach((month, count) -> builder.append(month).append(": ").append(count).append('\n'));
        message(builder.toString());
    }

    private void refreshIssues(List<Issue> issues) {
        issueListModel.clear();
        for (Issue issue : issues) {
            issueListModel.addElement(issue);
        }
        if (!issueListModel.isEmpty()) {
            issueList.setSelectedIndex(0);
        } else {
            detailArea.setText("");
        }
    }

    private void showSelectedIssue() {
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(issue.getId()).append('\n');
        builder.append("Title: ").append(issue.getTitle()).append('\n');
        builder.append("Description: ").append(issue.getDescription()).append('\n');
        builder.append("Project: ").append(issue.getProjectName()).append('\n');
        builder.append("Reporter: ").append(issue.getReporter()).append('\n');
        builder.append("Reported: ").append(issue.getReportedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append('\n');
        builder.append("Priority: ").append(issue.getPriority()).append('\n');
        builder.append("Status: ").append(issue.getStatus()).append('\n');
        builder.append("Assignee: ").append(value(issue.getAssignee())).append('\n');
        builder.append("Fixer: ").append(value(issue.getFixer())).append("\n\nComments\n");
        for (Comment comment : issue.getComments()) {
            builder.append(comment).append('\n');
        }
        detailArea.setText(builder.toString());
    }

    private Issue selectedIssue() {
        Issue issue = issueList.getSelectedValue();
        if (issue == null) {
            message("Select an issue first.");
        }
        return issue;
    }

    private String input(String label) {
        return JOptionPane.showInputDialog(this, label);
    }

    private void message(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            message(e.getMessage());
        }
    }

    private static String value(String value) {
        return value == null ? "-" : value;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SwingIssueApp(AppFactory.createController()).setVisible(true));
    }
}
