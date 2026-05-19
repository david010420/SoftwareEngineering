package its.ui.swing;

import its.AppFactory;
import its.controller.IssueController;
import its.model.Comment;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Role;
import its.model.UserAccount;
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
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SwingIssueApp extends JFrame {
    private final IssueController controller;
    private final DefaultListModel<Issue> issueListModel = new DefaultListModel<>();
    private final JList<Issue> issueList = new JList<>(issueListModel);
    private final JLabel ticketTitleLabel = new JLabel("No ticket selected");
    private final JTextArea ticketPropertiesArea = new JTextArea();
    private final JTextArea descriptionArea = new JTextArea();
    private final JTextArea commentsArea = new JTextArea();
    private final JTextField queryField = new JTextField();
    private final JTextField reporterField = new JTextField();
    private final JTextField assigneeField = new JTextField();
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final JLabel currentUserLabel = new JLabel("Not logged in");
    private final JLabel selectedTicketLabel = new JLabel("Selected ticket: none");
    private final List<JButton> loginRequiredButtons = new ArrayList<>();
    private UserAccount currentUser;

    public SwingIssueApp(IssueController controller) {
        super("Issue Management System - Swing");
        this.controller = controller;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 720);
        buildUi();
        refreshIssues(controller.issues());
    }

    private void buildUi() {
        JPanel loginPanel = new JPanel(new GridLayout(1, 2, 6, 6));
        JButton switchUserButton = new JButton("Switch User");
        switchUserButton.addActionListener(e -> switchUser());
        loginPanel.add(currentUserLabel);
        loginPanel.add(switchUserButton);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Browse", buildBrowseTab());
        tabs.addTab("New Issue", buildNewIssueTab());
        tabs.addTab("Workflow", buildWorkflowTab());
        tabs.addTab("Reports & Admin", buildReportsTab());

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(loginPanel, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        updateLoginState();
    }

    private JPanel buildBrowseTab() {
        JPanel filters = new JPanel(new GridLayout(5, 2, 6, 6));
        filters.setBorder(BorderFactory.createTitledBorder("Ticket Query"));
        statusBox.addItem("");
        for (IssueStatus status : IssueStatus.values()) {
            statusBox.addItem(status.name());
        }
        filters.add(new JLabel("Query"));
        filters.add(queryField);
        filters.add(new JLabel("Reporter"));
        filters.add(reporterField);
        filters.add(new JLabel("Assignee"));
        filters.add(assigneeField);
        filters.add(new JLabel("Status"));
        filters.add(statusBox);
        filters.add(new JLabel(""));
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> search());
        filters.add(searchButton);

        JPanel ticketListPanel = new JPanel(new BorderLayout(6, 6));
        ticketListPanel.setBorder(BorderFactory.createTitledBorder("Tickets"));
        ticketListPanel.add(new JScrollPane(issueList), BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(filters, BorderLayout.NORTH);
        leftPanel.add(ticketListPanel, BorderLayout.CENTER);

        JPanel rightPanel = buildTicketDetailPanel();
        issueList.addListSelectionListener(e -> showSelectedIssue());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(330);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildNewIssueTab() {
        JTextField titleField = new JTextField();
        JTextArea descriptionField = new JTextArea(10, 40);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        JComboBox<Priority> priorityBox = new JComboBox<>(Priority.values());
        priorityBox.setSelectedItem(Priority.MAJOR);

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(2, 2, 6, 6));
        fields.add(new JLabel("Title"));
        fields.add(titleField);
        fields.add(new JLabel("Priority"));
        fields.add(priorityBox);
        form.add(fields, BorderLayout.NORTH);
        form.add(new JScrollPane(descriptionField), BorderLayout.CENTER);

        JButton createButton = new JButton("Create Issue");
        createButton.addActionListener(e -> runSafely(() -> {
            requireLogin();
            String title = titleField.getText().trim();
            String description = descriptionField.getText().trim();
            if (title.isBlank() || description.isBlank()) {
                throw new IllegalStateException("Title and description are required.");
            }
            controller.createIssue("project1", title, description, currentUser.getUsername(), (Priority) priorityBox.getSelectedItem());
            titleField.setText("");
            descriptionField.setText("");
            refreshIssues(controller.issues());
            message("Issue created.");
        }));
        loginRequiredButtons.add(createButton);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(form, BorderLayout.CENTER);
        panel.add(createButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWorkflowTab() {
        JPanel actions = new JPanel(new GridLayout(2, 4, 6, 6));
        actions.setBorder(BorderFactory.createTitledBorder("Ticket Workflow"));
        addLoginRequiredButton(actions, "Comment", this::addComment);
        addLoginRequiredButton(actions, "Assign", this::assign);
        addLoginRequiredButton(actions, "Fix", this::fix);
        addLoginRequiredButton(actions, "Resolve", () -> changeStatus(IssueStatus.RESOLVED));
        addLoginRequiredButton(actions, "Close", () -> changeStatus(IssueStatus.CLOSED));
        addLoginRequiredButton(actions, "Reopen", () -> changeStatus(IssueStatus.REOPENED));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        selectedTicketLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
        panel.add(selectedTicketLabel, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReportsTab() {
        JPanel actions = new JPanel(new GridLayout(2, 2, 6, 6));
        actions.setBorder(BorderFactory.createTitledBorder("Reports & Admin"));
        addLoginRequiredButton(actions, "Add User", this::addUser);
        addButton(actions, "Recommend Assignee", this::recommend);
        addButton(actions, "Stats", this::stats);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(actions, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildTicketDetailPanel() {
        ticketTitleLabel.setFont(ticketTitleLabel.getFont().deriveFont(Font.BOLD, 18f));
        ticketTitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 8, 6));

        configureReadOnly(ticketPropertiesArea);
        configureReadOnly(descriptionArea);
        configureReadOnly(commentsArea);

        JPanel propertiesPanel = new JPanel(new BorderLayout());
        propertiesPanel.setBorder(BorderFactory.createTitledBorder("Properties"));
        propertiesPanel.add(new JScrollPane(ticketPropertiesArea), BorderLayout.CENTER);

        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
        descriptionPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        JPanel commentsPanel = new JPanel(new BorderLayout());
        commentsPanel.setBorder(BorderFactory.createTitledBorder("Change History"));
        commentsPanel.add(new JScrollPane(commentsArea), BorderLayout.CENTER);

        JSplitPane lowerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, descriptionPanel, commentsPanel);
        lowerSplit.setResizeWeight(0.45);

        JPanel detailBody = new JPanel(new BorderLayout(8, 8));
        detailBody.add(propertiesPanel, BorderLayout.NORTH);
        detailBody.add(lowerSplit, BorderLayout.CENTER);

        JPanel detailPanel = new JPanel(new BorderLayout(8, 8));
        detailPanel.setBorder(BorderFactory.createTitledBorder("Ticket Detail"));
        detailPanel.add(ticketTitleLabel, BorderLayout.NORTH);
        detailPanel.add(detailBody, BorderLayout.CENTER);
        return detailPanel;
    }

    private void configureReadOnly(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> runSafely(action));
        panel.add(button);
    }

    private void addLoginRequiredButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> runSafely(action));
        loginRequiredButtons.add(button);
        panel.add(button);
    }

    public boolean loginBeforeShow() {
        UserAccount selected = selectUser("Login", true);
        if (selected == null) {
            return false;
        }
        currentUser = selected;
        updateLoginState();
        return true;
    }

    private void switchUser() {
        UserAccount selected = selectUser("Switch User", false);
        if (selected != null) {
            currentUser = selected;
            updateLoginState();
        }
    }

    private UserAccount selectUser(String title, boolean required) {
        if (controller.users().isEmpty()) {
            message("No account exists. Demo data should create accounts automatically.");
            return null;
        }
        JTextField usernameField = new JTextField(currentUser == null ? "" : currentUser.getUsername());
        JPasswordField passwordField = new JPasswordField();
        JPanel loginForm = new JPanel(new GridLayout(2, 2, 6, 6));
        loginForm.add(new JLabel("Username"));
        loginForm.add(usernameField);
        loginForm.add(new JLabel("Password"));
        loginForm.add(passwordField);

        while (true) {
            int option = JOptionPane.showConfirmDialog(this, loginForm, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                if (required) {
                    message("Login is required to use the system.");
                }
                return null;
            }
            try {
                return controller.login(usernameField.getText(), new String(passwordField.getPassword()));
            } catch (RuntimeException e) {
                message(e.getMessage());
                passwordField.setText("");
            }
        }
    }

    private void updateLoginState() {
        if (currentUser == null) {
            currentUserLabel.setText("Not logged in");
        } else {
            currentUserLabel.setText("Logged in: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        }
        for (JButton button : loginRequiredButtons) {
            button.setEnabled(currentUser != null);
        }
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
        requireRole(Role.ADMIN, "Only admin can add users.");
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
        requireLogin();
        String title = input("Title");
        String description = input("Description");
        Priority priority = (Priority) JOptionPane.showInputDialog(this, "Priority", "New Issue",
                JOptionPane.PLAIN_MESSAGE, null, Priority.values(), Priority.MAJOR);
        if (title != null && description != null && priority != null) {
            controller.createIssue("project1", title, description, currentUser.getUsername(), priority);
            refreshIssues(controller.issues());
        }
    }

    private void addComment() {
        requireLogin();
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String message = input("Comment");
        if (message != null) {
            controller.addComment(issue.getId(), currentUser.getUsername(), message);
            refreshIssues(controller.issues());
        }
    }

    private void assign() {
        requireRole(Role.PL, "Only PL can assign issues.");
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String assignee = input("Assignee dev account");
        String comment = input("Comment");
        if (assignee != null) {
            controller.assignIssue(issue.getId(), assignee, currentUser.getUsername(), comment == null ? "" : comment);
            refreshIssues(controller.issues());
        }
    }

    private void fix() {
        requireRole(Role.DEV, "Only dev can mark an issue fixed.");
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String comment = input("Comment");
        controller.markFixed(issue.getId(), currentUser.getUsername(), comment == null ? "" : comment);
        refreshIssues(controller.issues());
    }

    private void changeStatus(IssueStatus status) {
        requireLogin();
        if (status == IssueStatus.CLOSED) {
            requireRole(Role.PL, "Only PL can close issues.");
        }
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String comment = input("Comment");
        controller.changeStatus(issue.getId(), status, currentUser.getUsername(), comment == null ? "" : comment);
        refreshIssues(controller.issues());
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
            clearTicketDetail();
        }
    }

    private void showSelectedIssue() {
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        selectedTicketLabel.setText("Selected ticket: #" + issue.getId() + " " + issue.getTitle());
        ticketTitleLabel.setText("#" + issue.getId() + " " + issue.getTitle());
        StringBuilder properties = new StringBuilder();
        properties.append("Project  : ").append(issue.getProjectName()).append('\n');
        properties.append("Status   : ").append(issue.getStatus()).append('\n');
        properties.append("Priority : ").append(issue.getPriority()).append('\n');
        properties.append("Reporter : ").append(issue.getReporter()).append('\n');
        properties.append("Reported : ").append(issue.getReportedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append('\n');
        properties.append("Assignee : ").append(value(issue.getAssignee())).append('\n');
        properties.append("Fixer    : ").append(value(issue.getFixer()));
        ticketPropertiesArea.setText(properties.toString());
        ticketPropertiesArea.setCaretPosition(0);

        descriptionArea.setText(issue.getDescription());
        descriptionArea.setCaretPosition(0);

        StringBuilder comments = new StringBuilder();
        for (Comment comment : issue.getComments()) {
            comments.append(comment).append('\n');
        }
        commentsArea.setText(comments.isEmpty() ? "No comments yet." : comments.toString());
        commentsArea.setCaretPosition(0);
    }

    private void clearTicketDetail() {
        selectedTicketLabel.setText("Selected ticket: none");
        ticketTitleLabel.setText("No ticket selected");
        ticketPropertiesArea.setText("");
        descriptionArea.setText("");
        commentsArea.setText("");
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

    private void requireLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("Login first.");
        }
    }

    private void requireRole(Role role, String message) {
        requireLogin();
        if (currentUser.getRole() != role) {
            throw new IllegalStateException(message);
        }
    }

    private static String value(String value) {
        return value == null ? "-" : value;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SwingIssueApp app = new SwingIssueApp(AppFactory.createController());
            if (app.loginBeforeShow()) {
                app.setVisible(true);
            } else {
                app.dispose();
            }
        });
    }
}
