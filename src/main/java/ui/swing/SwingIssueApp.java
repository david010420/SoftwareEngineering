package ui.swing;

import app.AppFactory;
import controller.IssueController;
import controller.ProjectController;
import controller.UserController;
import model.Issue;
import model.IssueComment;
import model.IssueStatus;
import model.Priority;
import model.Project;
import model.Role;
import model.UserAccount;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SwingIssueApp extends JFrame {
    private final IssueController controller;
    private final ProjectController projectController;
    private final UserController userController;
    private final DefaultTableModel issueTableModel = new DefaultTableModel(
            new String[]{"ID", "Project", "Status", "Priority", "Title", "Assignee", "Reporter", "Updated"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable issueTable = new JTable(issueTableModel);
    private final List<Issue> visibleIssues = new ArrayList<>();
    private final JLabel ticketTitleLabel = new JLabel("No ticket selected");
    private final JTextArea ticketPropertiesArea = new JTextArea();
    private final JTextArea descriptionArea = new JTextArea();
    private final JTextArea commentsArea = new JTextArea();
    private final JTextField queryField = new JTextField();
    private final JTextField projectFilterField = new JTextField();
    private final JTextField reporterField = new JTextField();
    private final JTextField assigneeField = new JTextField();
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final JComboBox<Project> projectBox = new JComboBox<>();
    private final JLabel currentUserLabel = new JLabel("Not logged in");
    private final JLabel selectedTicketLabel = new JLabel("Selected ticket: none");
    private final JLabel workflowModeLabel = new JLabel("Choose an action.");
    private final JLabel workflowAssigneeLabel = new JLabel("Assignee");
    private final JTextField workflowAssigneeField = new JTextField();
    private final JLabel workflowCommentLabel = new JLabel("Comment");
    private final JTextArea workflowCommentArea = new JTextArea(8, 40);
    private final JButton workflowApplyButton = new JButton("Apply");
    private final JTextArea recommendationArea = new JTextArea();
    private final JTextArea statsArea = new JTextArea();
    private final JLabel statusMessageLabel = new JLabel("Ready.");
    private final List<RoleAction> roleActions = new ArrayList<>();
    private JTabbedPane tabs;
    private JPanel browseTab;
    private JPanel newIssueTab;
    private JPanel workflowTab;
    private JPanel reportsTab;
    private JPanel adminTab;
    private UserAccount currentUser;
    private Long selectedIssueId;
    private Runnable selectedWorkflowAction;

    public SwingIssueApp(IssueController controller, ProjectController projectController, UserController userController) {
        super("Issue Management System - Swing");
        this.controller = controller;
        this.projectController = projectController;
        this.userController = userController;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 720));
        setSize(1280, 820);
        buildUi();
        refreshIssues(controller.issues());
    }

    private void buildUi() {
        JPanel loginPanel = new JPanel(new BorderLayout(8, 0));
        JButton switchUserButton = new JButton("Switch User");
        switchUserButton.addActionListener(e -> switchUser());
        currentUserLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        loginPanel.add(currentUserLabel, BorderLayout.CENTER);
        loginPanel.add(switchUserButton, BorderLayout.EAST);

        tabs = new JTabbedPane();
        browseTab = buildBrowseTab();
        newIssueTab = buildNewIssueTab();
        workflowTab = buildWorkflowTab();
        reportsTab = buildReportsTab();
        adminTab = buildAdminTab();

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(loginPanel, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusMessageLabel, BorderLayout.SOUTH);
        setContentPane(root);
        updateLoginState();
    }

    private JPanel buildBrowseTab() {
        JPanel filters = new JPanel(new GridLayout(6, 2, 6, 6));
        filters.setBorder(BorderFactory.createTitledBorder("Ticket Query"));
        statusBox.addItem("ALL");
        for (IssueStatus status : IssueStatus.values()) {
            statusBox.addItem(status.name());
        }
        filters.add(new JLabel("Project"));
        filters.add(projectFilterField);
        filters.add(new JLabel("Query"));
        filters.add(queryField);
        filters.add(new JLabel("Reporter"));
        filters.add(reporterField);
        filters.add(new JLabel("Assignee"));
        filters.add(assigneeField);
        filters.add(new JLabel("Status"));
        filters.add(statusBox);
        filters.add(new JLabel(""));
        JPanel searchActions = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> search());
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetSearch());
        searchActions.add(searchButton);
        searchActions.add(resetButton);
        filters.add(searchActions);

        JPanel quickFilters = new JPanel(new GridLayout(4, 2, 6, 6));
        quickFilters.setBorder(BorderFactory.createTitledBorder("Quick Filters"));
        addQuickFilterButton(quickFilters, "All", () -> applyQuickFilter(null, null, null));
        addQuickFilterButton(quickFilters, "Open", () -> applyOpenFilter());
        addQuickFilterButton(quickFilters, "New", () -> applyQuickFilter(IssueStatus.NEW, null, null));
        addQuickFilterButton(quickFilters, "Assigned to Me", () -> {
            requireLogin();
            applyQuickFilter(null, null, currentUser.getUsername());
        });
        addQuickFilterButton(quickFilters, "Reported by Me", () -> {
            requireLogin();
            applyQuickFilter(null, currentUser.getUsername(), null);
        });
        addQuickFilterButton(quickFilters, "Fixed", () -> applyQuickFilter(IssueStatus.FIXED, null, null));
        addQuickFilterButton(quickFilters, "Resolved", () -> applyQuickFilter(IssueStatus.RESOLVED, null, null));
        addQuickFilterButton(quickFilters, "Closed", () -> applyQuickFilter(IssueStatus.CLOSED, null, null));

        JPanel ticketListPanel = new JPanel(new BorderLayout(6, 6));
        ticketListPanel.setBorder(BorderFactory.createTitledBorder("Tickets"));
        configureIssueTable();
        ticketListPanel.add(new JScrollPane(issueTable), BorderLayout.CENTER);

        JPanel filterPanel = new JPanel(new BorderLayout(6, 6));
        filterPanel.add(filters, BorderLayout.NORTH);
        filterPanel.add(quickFilters, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.add(filterPanel, BorderLayout.NORTH);
        leftPanel.add(ticketListPanel, BorderLayout.CENTER);

        JPanel rightPanel = buildTicketDetailPanel();
        issueTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedIssue();
            }
        });
        leftPanel.setMinimumSize(new Dimension(380, 300));
        rightPanel.setMinimumSize(new Dimension(560, 300));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setResizeWeight(0.34);
        split.setDividerLocation(400);

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
            controller.createIssue(selectedProject.getId(), title, description, currentUser.getUsername(), (Priority) priorityBox.getSelectedItem());
            titleField.setText("");
            descriptionField.setText("");
            refreshIssues(controller.issues());
            showStatus("Issue created.");
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
        actions.setBorder(BorderFactory.createTitledBorder("Issue Actions"));
        addWorkflowButton(actions, "Comment", false, this::addComment,
                new IssueStatus[0], Role.ADMIN, Role.PL, Role.DEV, Role.TESTER);
        addWorkflowButton(actions, "Assign", true, this::assign,
                new IssueStatus[]{IssueStatus.NEW, IssueStatus.REOPENED}, Role.PL);
        addWorkflowButton(actions, "Fix", false, this::fix,
                new IssueStatus[]{IssueStatus.ASSIGNED, IssueStatus.REOPENED}, Role.DEV);
        addWorkflowButton(actions, "Resolve", false, () -> changeStatus(IssueStatus.RESOLVED),
                new IssueStatus[]{IssueStatus.FIXED}, Role.TESTER);
        addWorkflowButton(actions, "Close", false, () -> changeStatus(IssueStatus.CLOSED),
                new IssueStatus[]{IssueStatus.RESOLVED}, Role.PL);
        addWorkflowButton(actions, "Reopen", false, () -> changeStatus(IssueStatus.REOPENED),
                new IssueStatus[]{IssueStatus.RESOLVED, IssueStatus.CLOSED}, Role.TESTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        selectedTicketLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 8, 4));
        selectedTicketLabel.setPreferredSize(new Dimension(800, 28));

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.add(selectedTicketLabel, BorderLayout.NORTH);
        header.add(actions, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(buildWorkflowInputPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildWorkflowInputPanel() {
        workflowCommentArea.setLineWrap(true);
        workflowCommentArea.setWrapStyleWord(true);
        workflowApplyButton.setEnabled(false);
        workflowApplyButton.addActionListener(e -> runSafely(() -> {
            if (selectedWorkflowAction == null) {
                throw new IllegalStateException("Choose an action first.");
            }
            selectedWorkflowAction.run();
        }));

        JPanel assigneeRow = new JPanel(new BorderLayout(8, 0));
        assigneeRow.add(workflowAssigneeLabel, BorderLayout.WEST);
        assigneeRow.add(workflowAssigneeField, BorderLayout.CENTER);

        JPanel commentPanel = new JPanel(new BorderLayout(8, 8));
        commentPanel.add(workflowCommentLabel, BorderLayout.NORTH);
        commentPanel.add(new JScrollPane(workflowCommentArea), BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(workflowModeLabel, BorderLayout.NORTH);
        topPanel.add(assigneeRow, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Action Input"));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(commentPanel, BorderLayout.CENTER);
        panel.add(workflowApplyButton, BorderLayout.SOUTH);
        showWorkflowAssigneeInput(false);
        return panel;
    }

    private JPanel buildReportsTab() {
        JPanel reportActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        reportActions.setBorder(BorderFactory.createTitledBorder("Reports"));
        addRoleButton(reportActions, "Recommend Assignee", this::recommend, Role.PL);
        addRoleButton(reportActions, "Stats", this::stats, Role.ADMIN, Role.PL);

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
        panel.add(reportActions, BorderLayout.NORTH);
        panel.add(resultSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAdminTab() {
        JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        adminActions.setBorder(BorderFactory.createTitledBorder("Admin"));
        addRoleButton(adminActions, "Add User", this::addUser, Role.ADMIN);
        addRoleButton(adminActions, "Add Project", this::addProject, Role.ADMIN);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(adminActions, BorderLayout.NORTH);
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

    private void configureIssueTable() {
        issueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        issueTable.setAutoCreateRowSorter(true);
        issueTable.setRowHeight(24);
        issueTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        issueTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        issueTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        issueTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        issueTable.getColumnModel().getColumn(4).setPreferredWidth(250);
        issueTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        issueTable.getColumnModel().getColumn(6).setPreferredWidth(95);
        issueTable.getColumnModel().getColumn(7).setPreferredWidth(135);
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

    private void addWorkflowButton(JPanel panel, String text, boolean needsAssignee, Runnable action,
                                   IssueStatus[] allowedStatuses, Role... allowedRoles) {
        JButton button = new JButton(text);
        button.addActionListener(e -> selectWorkflowAction(text, needsAssignee, action));
        roleActions.add(new RoleAction(button, allowedStatuses, allowedRoles));
        panel.add(button);
    }

    private void addQuickFilterButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusable(false);
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
        if (userController.findAll().isEmpty()) {
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
                return userController.login(usernameField.getText(), new String(passwordField.getPassword()));
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
            action.button.setVisible(action.isAvailableFor(currentUser, currentIssue()));
        }
        resetWorkflowAction();
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
            tabs.addTab("Issue Actions", workflowTab);
        }
        if (currentUser != null && hasAnyActionFor(reportsTab, currentUser.getRole())) {
            tabs.addTab("Reports", reportsTab);
        }
        if (currentUser != null && hasAnyActionFor(adminTab, currentUser.getRole())) {
            tabs.addTab("Admin", adminTab);
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
        String selectedStatus = statusBox.getSelectedItem() == null ? "ALL" : statusBox.getSelectedItem().toString();
        IssueStatus status = "ALL".equals(selectedStatus) ? null : IssueStatus.valueOf(selectedStatus);
        List<Issue> results = controller.search(queryField.getText(), reporterField.getText(), assigneeField.getText(), status);
        String projectFilter = projectFilterField.getText();
        if (projectFilter != null && !projectFilter.isBlank()) {
            String normalizedProject = projectFilter.trim();
            results = results.stream()
                    .filter(issue -> projectNameOf(issue).equalsIgnoreCase(normalizedProject)
                            || String.valueOf(issue.getProjectId()).equals(normalizedProject))
                    .toList();
        }
        refreshIssues(results);
    }

    private void applyQuickFilter(IssueStatus status, String reporter, String assignee) {
        projectFilterField.setText("");
        queryField.setText("");
        reporterField.setText(reporter == null ? "" : reporter);
        assigneeField.setText(assignee == null ? "" : assignee);
        statusBox.setSelectedItem(status == null ? "ALL" : status.name());
        search();
    }

    private void applyOpenFilter() {
        projectFilterField.setText("");
        queryField.setText("");
        reporterField.setText("");
        assigneeField.setText("");
        statusBox.setSelectedItem("ALL");
        refreshIssues(controller.issues().stream()
                .filter(issue -> issue.getStatus() != IssueStatus.CLOSED && issue.getStatus() != IssueStatus.RESOLVED)
                .toList());
    }

    private void resetSearch() {
        projectFilterField.setText("");
        queryField.setText("");
        reporterField.setText("");
        assigneeField.setText("");
        statusBox.setSelectedItem("ALL");
        refreshIssues(controller.issues());
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
            userController.register(currentUser.getUsername(), username, "1234", role);
            showStatus("User added. Default password is 1234.");
        }
    }

    private void addProject() {
        requireRole(Role.ADMIN, "Only admin can add projects.");
        String projectName = input("Project name");
        if (projectName == null || projectName.isBlank()) {
            return;
        }
        projectController.createProject(projectName);
        refreshProjectChoices();
        showStatus("Project added.");
    }

    private void refreshProjectChoices() {
        Project selected = (Project) projectBox.getSelectedItem();
        projectBox.removeAllItems();
        for (Project project : projectController.findAllProjects()) {
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
            Project selectedProject = (Project) projectBox.getSelectedItem();
            if (selectedProject == null) {
                throw new IllegalStateException("Project is required.");
            }
            controller.createIssue(selectedProject.getId(), title, description, currentUser.getUsername(), priority);
            refreshIssues(controller.issues());
        }
    }

    private void addComment() {
        requireLogin();
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String comment = workflowComment();
        if (comment.isBlank()) {
            throw new IllegalStateException("Comment is required.");
        }
        controller.addComment(issue.getId(), currentUser.getUsername(), comment);
        clearWorkflowInputs();
        refreshIssues(controller.issues());
        showStatus("Comment added to issue #" + issue.getId() + ".");
    }

    private void assign() {
        requireRole(Role.PL, "Only PL can assign issues.");
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        String assignee = workflowAssignee();
        if (assignee.isBlank()) {
            throw new IllegalStateException("Assignee is required.");
        }
        controller.assignIssue(issue.getId(), assignee, currentUser.getUsername(), workflowComment());
        clearWorkflowInputs();
        refreshIssues(controller.issues());
        showStatus("Issue #" + issue.getId() + " assigned to " + assignee + ".");
    }

    private void fix() {
        requireRole(Role.DEV, "Only dev can mark an issue fixed.");
        Issue issue = selectedIssue();
        if (issue == null) {
            return;
        }
        controller.markFixed(issue.getId(), currentUser.getUsername(), workflowComment());
        clearWorkflowInputs();
        refreshIssues(controller.issues());
        showStatus("Issue #" + issue.getId() + " marked fixed.");
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
        controller.changeStatus(issue.getId(), status, currentUser.getUsername(), workflowComment());
        clearWorkflowInputs();
        refreshIssues(controller.issues());
        showStatus("Issue #" + issue.getId() + " changed to " + status + ".");
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
        StringBuilder builder = new StringBuilder();
        builder.append("Daily Issue Counts\n");
        builder.append(String.format("%-14s %s%n", "Date", "Count"));
        builder.append("----------------------\n");
        controller.statistics().getDailyCounts().forEach((day, count) ->
                builder.append(String.format("%-14s %d%n", day, count)));
        builder.append("\nMonthly Issue Counts\n");
        builder.append(String.format("%-14s %s%n", "Month", "Count"));
        builder.append("----------------------\n");
        controller.statistics().getMonthlyCounts().forEach((month, count) ->
                builder.append(String.format("%-14s %d%n", month, count)));
        statsArea.setText(builder.toString());
        statsArea.setCaretPosition(0);
    }

    private void refreshIssues(List<Issue> issues) {
        Long previousSelection = selectedIssueId;
        visibleIssues.clear();
        visibleIssues.addAll(issues);
        issueTableModel.setRowCount(0);
        for (Issue issue : issues) {
            issueTableModel.addRow(new Object[]{
                    issue.getId(),
                    projectNameOf(issue),
                    issue.getStatus(),
                    issue.getPriority(),
                    issue.getTitle(),
                    value(issue.getAssignee()),
                    issue.getReporter(),
                    latestActivity(issue)
            });
        }
        if (!visibleIssues.isEmpty()) {
            int rowToSelect = 0;
            if (previousSelection != null) {
                for (int i = 0; i < visibleIssues.size(); i++) {
                    if (visibleIssues.get(i).getId() == previousSelection) {
                        rowToSelect = i;
                        break;
                    }
                }
            }
            selectedIssueId = visibleIssues.get(rowToSelect).getId();
            issueTable.setRowSelectionInterval(rowToSelect, rowToSelect);
            showSelectedIssue();
        } else {
            selectedIssueId = null;
            clearTicketDetail();
        }
    }

    private void showSelectedIssue() {
        Issue issue = selectedIssueFromTable();
        if (issue == null) {
            return;
        }
        selectedIssueId = issue.getId();
        updateActionVisibility();
        selectedTicketLabel.setText("Selected ticket: #" + issue.getId() + " " + issue.getTitle());
        ticketTitleLabel.setText("#" + issue.getId() + " " + issue.getTitle());
        StringBuilder properties = new StringBuilder();
        properties.append(String.format("%-10s %-18s %-10s %s%n", "Project", projectNameOf(issue), "Status", issue.getStatus()));
        properties.append(String.format("%-10s %-18s %-10s %s%n", "Priority", issue.getPriority(), "Reporter", issue.getReporter()));
        properties.append(String.format("%-10s %-18s %-10s %s%n", "Assignee", value(issue.getAssignee()), "Fixer", value(issue.getFixer())));
        properties.append(String.format("%-10s %-18s %-10s %s", "Reported", issue.getReportedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "Updated", latestActivity(issue)));
        ticketPropertiesArea.setText(properties.toString());
        ticketPropertiesArea.setCaretPosition(0);

        descriptionArea.setText(issue.getDescription());
        descriptionArea.setCaretPosition(0);

        StringBuilder comments = new StringBuilder();
        for (IssueComment comment : issue.getComments()) {
            comments.append("[")
                    .append(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append("] ")
                    .append(comment.getAuthorUsername())
                    .append(": ")
                    .append(comment.getBody())
                    .append('\n');
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
        updateActionVisibility();
    }

    private Issue selectedIssue() {
        Issue tableIssue = selectedIssueFromTable();
        if (tableIssue != null) {
            selectedIssueId = tableIssue.getId();
            return tableIssue;
        }
        if (selectedIssueId != null) {
            for (Issue issue : visibleIssues) {
                if (issue.getId() == selectedIssueId) {
                    return issue;
                }
            }
            try {
                return controller.getIssue(selectedIssueId);
            } catch (RuntimeException ignored) {
                selectedIssueId = null;
            }
        }
        message("Select an issue first.");
        return null;
    }

    private Issue selectedIssueFromTable() {
        int row = issueTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int modelRow = issueTable.convertRowIndexToModel(row);
        if (modelRow < 0 || modelRow >= visibleIssues.size()) {
            return null;
        }
        return visibleIssues.get(modelRow);
    }

    private Issue currentIssue() {
        if (selectedIssueId == null) {
            return null;
        }
        for (Issue issue : visibleIssues) {
            if (issue.getId() == selectedIssueId) {
                return issue;
            }
        }
        try {
            return controller.getIssue(selectedIssueId);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String input(String label) {
        return JOptionPane.showInputDialog(this, label);
    }

    private String workflowAssignee() {
        return workflowAssigneeField.getText().trim();
    }

    private String workflowComment() {
        return workflowCommentArea.getText().trim();
    }

    private void clearWorkflowInputs() {
        workflowAssigneeField.setText("");
        workflowCommentArea.setText("");
    }

    private void updateActionVisibility() {
        Issue issue = currentIssue();
        for (RoleAction action : roleActions) {
            action.button.setVisible(action.isAvailableFor(currentUser, issue));
        }
        resetWorkflowAction();
        revalidate();
        repaint();
    }

    private void selectWorkflowAction(String actionName, boolean needsAssignee, Runnable action) {
        selectedWorkflowAction = action;
        workflowModeLabel.setText("Selected action: " + actionName);
        workflowCommentLabel.setText(actionName + " comment");
        workflowApplyButton.setText("Apply " + actionName);
        workflowApplyButton.setEnabled(true);
        showWorkflowAssigneeInput(needsAssignee);
        if (!needsAssignee) {
            workflowAssigneeField.setText("");
        }
    }

    private void resetWorkflowAction() {
        selectedWorkflowAction = null;
        workflowModeLabel.setText("Choose an action.");
        workflowCommentLabel.setText("Comment");
        workflowApplyButton.setText("Apply");
        workflowApplyButton.setEnabled(false);
        showWorkflowAssigneeInput(false);
        clearWorkflowInputs();
    }

    private void showWorkflowAssigneeInput(boolean visible) {
        workflowAssigneeLabel.setVisible(visible);
        workflowAssigneeField.setVisible(visible);
    }

    private void showStatus(String message) {
        statusMessageLabel.setText(message);
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

    private String projectNameOf(Issue issue) {
        return projectController.findAllProjects().stream()
                .filter(project -> project.getId() == issue.getProjectId())
                .map(Project::getName)
                .findFirst()
                .orElse(String.valueOf(issue.getProjectId()));
    }

    private static String latestActivity(Issue issue) {
        if (issue.getComments().isEmpty()) {
            return issue.getReportedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        IssueComment latest = issue.getComments().get(issue.getComments().size() - 1);
        return latest.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static class RoleAction {
        private final JButton button;
        private final List<Role> allowedRoles;
        private final List<IssueStatus> allowedStatuses;
        private final boolean workflowAction;

        private RoleAction(JButton button, Role... allowedRoles) {
            this(button, null, allowedRoles);
        }

        private RoleAction(JButton button, IssueStatus[] allowedStatuses, Role... allowedRoles) {
            this.button = button;
            this.allowedRoles = Arrays.asList(allowedRoles);
            this.allowedStatuses = allowedStatuses == null ? List.of() : Arrays.asList(allowedStatuses);
            this.workflowAction = allowedStatuses != null;
        }

        private boolean allows(Role role) {
            return allowedRoles.contains(role);
        }

        private boolean isAvailableFor(UserAccount user, Issue issue) {
            if (user == null || !allows(user.getRole())) {
                return false;
            }
            if (!workflowAction) {
                return true;
            }
            if (issue == null) {
                return false;
            }
            return allowedStatuses.isEmpty() || allowedStatuses.contains(issue.getStatus());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppFactory.Controllers controllers = AppFactory.createControllers();
            SwingIssueApp app = new SwingIssueApp(
                    controllers.issueController(),
                    controllers.projectController(),
                    controllers.userController());
            if (app.loginBeforeShow()) {
                app.setVisible(true);
            } else {
                app.dispose();
            }
        });
    }
}
