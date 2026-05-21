package its.ui.swing;

import its.AppFactory;
import its.controller.IssueController;
import its.model.Comment;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.model.Project;
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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final JComboBox<Project> projectBox = new JComboBox<>();
    private final JLabel currentUserLabel = new JLabel("Not logged in");
    private final JLabel selectedTicketLabel = new JLabel("Selected ticket: none");
    private final JTextArea recommendationArea = new JTextArea();
    private final JTextArea statsArea = new JTextArea();
    private final List<RoleAction> roleActions = new ArrayList<>();
    private JTabbedPane tabs;
    private JPanel browseTab;
    private JPanel newIssueTab;
    private JPanel workflowTab;
    private JPanel reportsTab;
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

        tabs = new JTabbedPane();
        browseTab = buildBrowseTab();
        newIssueTab = buildNewIssueTab();
        workflowTab = buildWorkflowTab();
        reportsTab = buildReportsTab();

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

        JPanel quickFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        quickFilters.setBorder(BorderFactory.createTitledBorder("Quick Filters"));
        addQuickFilterButton(quickFilters, "All", () -> applyQuickFilter(null, null, null));
        addQuickFilterButton(quickFilters, "NEW", () -> applyQuickFilter(IssueStatus.NEW, null, null));
        addQuickFilterButton(quickFilters, "Assigned to Me", () -> {
            requireLogin();
            applyQuickFilter(null, null, currentUser.getUsername());
        });
        addQuickFilterButton(quickFilters, "Reported by Me", () -> {
            requireLogin();
            applyQuickFilter(null, currentUser.getUsername(), null);
        });
        addQuickFilterButton(quickFilters, "FIXED", () -> applyQuickFilter(IssueStatus.FIXED, null, null));
        addQuickFilterButton(quickFilters, "RESOLVED", () -> applyQuickFilter(IssueStatus.RESOLVED, null, null));

        JPanel ticketListPanel = new JPanel(new BorderLayout(6, 6));
        ticketListPanel.setBorder(BorderFactory.createTitledBorder("Tickets"));
        ticketListPanel.add(new JScrollPane(issueList), BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new BorderLayout(6, 6));
        filterPanel.add(filters, BorderLayout.NORTH);
        filterPanel.add(quickFilters, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(filterPanel, BorderLayout.NORTH);
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
        refreshProjectChoices();

        JPanel form = new JPanel(new BorderLayout(8, 8));
        JPanel fields = new JPanel(new GridLayout(3, 2, 6, 6));
        fields.add(new JLabel("Project"));
        fields.add(projectBox);
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
            Project selectedProject = (Project) projectBox.getSelectedItem();
            if (selectedProject == null) {
                throw new IllegalStateException("Project is required.");
            }
            controller.createIssue(selectedProject.getName(), title, description, currentUser.getUsername(), (Priority) priorityBox.getSelectedItem());
            titleField.setText("");
            descriptionField.setText("");
            refreshIssues(controller.issues());
            message("Issue created.");
        }));
        roleActions.add(new RoleAction(createButton, Role.TESTER));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(form, BorderLayout.CENTER);
        panel.add(createButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWorkflowTab() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setBorder(BorderFactory.createTitledBorder("Ticket Workflow"));
        addRoleButton(actions, "Comment", this::addComment, Role.ADMIN, Role.PL, Role.DEV, Role.TESTER);
        addRoleButton(actions, "Assign", this::assign, Role.PL);
        addRoleButton(actions, "Fix", this::fix, Role.DEV);
        addRoleButton(actions, "Resolve", () -> changeStatus(IssueStatus.RESOLVED), Role.TESTER);
        addRoleButton(actions, "Close", () -> changeStatus(IssueStatus.CLOSED), Role.PL);
        addRoleButton(actions, "Reopen", () -> changeStatus(IssueStatus.REOPENED), Role.TESTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        selectedTicketLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
        panel.add(selectedTicketLabel, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildReportsTab() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        actions.setBorder(BorderFactory.createTitledBorder("Reports & Admin"));
        addRoleButton(actions, "Add User", this::addUser, Role.ADMIN);
        addRoleButton(actions, "Add Project", this::addProject, Role.ADMIN);
        addRoleButton(actions, "Recommend Assignee", this::recommend, Role.PL);
        addRoleButton(actions, "Stats", this::stats, Role.ADMIN, Role.PL);

        configureReadOnly(recommendationArea);
        configureReadOnly(statsArea);
        recommendationArea.setText("Select a ticket and click Recommend Assignee.");
        statsArea.setText("Click Stats to view issue counts.");

        JPanel recommendationPanel = new JPanel(new BorderLayout());
        recommendationPanel.setBorder(BorderFactory.createTitledBorder("Recommendation Result"));
        recommendationPanel.add(new JScrollPane(recommendationArea), BorderLayout.CENTER);

        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistics Result"));
        statsPanel.add(new JScrollPane(statsArea), BorderLayout.CENTER);

        JSplitPane resultSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, recommendationPanel, statsPanel);
        resultSplit.setResizeWeight(0.5);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(actions, BorderLayout.NORTH);
        panel.add(resultSplit, BorderLayout.CENTER);
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

    private void addRoleButton(JPanel panel, String text, Runnable action, Role... allowedRoles) {
        JButton button = new JButton(text);
        button.addActionListener(e -> runSafely(action));
        roleActions.add(new RoleAction(button, allowedRoles));
        panel.add(button);
    }

    private void addQuickFilterButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(e -> runSafely(action));
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
        for (RoleAction action : roleActions) {
            action.button.setVisible(currentUser != null && action.allows(currentUser.getRole()));
        }
        rebuildVisibleTabs();
        revalidate();
        repaint();
    }

    private void rebuildVisibleTabs() {
        if (tabs == null) {
            return;
        }
        Component selected = tabs.getSelectedComponent();
        tabs.removeAll();
        tabs.addTab("Browse", browseTab);
        if (currentUser != null && currentUser.getRole() == Role.TESTER) {
            tabs.addTab("New Issue", newIssueTab);
        }
        if (currentUser != null && hasAnyActionFor(workflowTab, currentUser.getRole())) {
            tabs.addTab("Workflow", workflowTab);
        }
        if (currentUser != null && hasAnyActionFor(reportsTab, currentUser.getRole())) {
            tabs.addTab("Reports & Admin", reportsTab);
        }
        if (selected != null) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (tabs.getComponentAt(i) == selected) {
                    tabs.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private boolean hasAnyActionFor(Component root, Role role) {
        for (RoleAction action : roleActions) {
            if (isDescendant(root, action.button) && action.allows(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDescendant(Component root, Component child) {
        Component current = child;
        while (current != null) {
            if (current == root) {
                return true;
            }
            current = current.getParent();
        }
        return false;
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

    private void applyQuickFilter(IssueStatus status, String reporter, String assignee) {
        queryField.setText("");
        reporterField.setText(reporter == null ? "" : reporter);
        assigneeField.setText(assignee == null ? "" : assignee);
        statusBox.setSelectedItem(status == null ? "" : status.name());
        search();
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

    private void addProject() {
        requireRole(Role.ADMIN, "Only admin can add projects.");
        String projectName = input("Project name");
        if (projectName == null || projectName.isBlank()) {
            return;
        }
        controller.addProject(projectName);
        refreshProjectChoices();
        message("Project added.");
    }

    private void refreshProjectChoices() {
        Project selected = (Project) projectBox.getSelectedItem();
        projectBox.removeAllItems();
        for (Project project : controller.projects()) {
            projectBox.addItem(project);
            if (selected != null && selected.getName().equalsIgnoreCase(project.getName())) {
                projectBox.setSelectedItem(project);
            }
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
            StringBuilder builder = new StringBuilder();
            builder.append("Selected issue: #").append(issue.getId()).append(" ").append(issue.getTitle()).append('\n');
            builder.append("Status: ").append(issue.getStatus()).append('\n');
            builder.append("Priority: ").append(issue.getPriority()).append("\n\n");
            if (candidates.isEmpty()) {
                builder.append("No candidate yet.");
            } else {
                builder.append("Best candidates:\n");
                for (int i = 0; i < candidates.size(); i++) {
                    builder.append(i + 1).append(". ").append(candidates.get(i)).append('\n');
                }
            }
            recommendationArea.setText(builder.toString());
            recommendationArea.setCaretPosition(0);
        }
    }

    private void stats() {
        StringBuilder builder = new StringBuilder("Daily\n");
        controller.statistics().getDailyCounts().forEach((day, count) -> builder.append(day).append(": ").append(count).append('\n'));
        builder.append("\nMonthly\n");
        controller.statistics().getMonthlyCounts().forEach((month, count) -> builder.append(month).append(": ").append(count).append('\n'));
        statsArea.setText(builder.toString());
        statsArea.setCaretPosition(0);
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

    private static class RoleAction {
        private final JButton button;
        private final List<Role> allowedRoles;

        private RoleAction(JButton button, Role... allowedRoles) {
            this.button = button;
            this.allowedRoles = Arrays.asList(allowedRoles);
        }

        private boolean allows(Role role) {
            return allowedRoles.contains(role);
        }
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
