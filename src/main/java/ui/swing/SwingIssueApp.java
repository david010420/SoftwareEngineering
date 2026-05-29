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
import service.IssueStatistics;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final JLabel ticketTitleLabel = new JLabel("No issue selected");
    private final JTextArea ticketPropertiesArea = new JTextArea();
    private final JTextArea descriptionArea = new JTextArea();
    private final JTextArea commentsArea = new JTextArea();
    private final JTextField queryField = new JTextField();
    private final JTextField projectFilterField = new JTextField();
    private final JTextField reporterField = new JTextField();
    private final JTextField assigneeField = new JTextField();
    private final JComboBox<String> statusBox = new JComboBox<>();
    private final JComboBox<String> priorityFilterBox = new JComboBox<>();
    private final JComboBox<Project> projectBox = new JComboBox<>();
    private final JLabel currentUserLabel = new JLabel("Not logged in");
    private final JLabel selectedTicketLabel = new JLabel("Selected issue: none");
    private final JLabel workflowModeLabel = new JLabel("Choose an action.");
    private final JLabel workflowAssigneeLabel = new JLabel("Assignee");
    private final JComboBox<String> workflowAssigneeBox = new JComboBox<>();
    private final JLabel workflowCommentLabel = new JLabel("Comment");
    private final JTextArea workflowCommentArea = new JTextArea(8, 40);
    private final JButton workflowApplyButton = new JButton("Apply");
    private final JPanel assignmentSupportPanel = new JPanel(new BorderLayout(8, 8));
    private final JTextArea recommendationArea = new JTextArea();
    private final JTextArea developerArea = new JTextArea();
    private final JTextArea statsArea = new JTextArea();
    private final JPanel reportSummaryPanel = new JPanel(new GridLayout(1, 5, 8, 0));
    private final JPanel statusBreakdownPanel = new JPanel();
    private final JPanel priorityBreakdownPanel = new JPanel();
    private final JPanel workloadPanel = new JPanel();
    private final TrendChartPanel trendChartPanel = new TrendChartPanel();
    private final JTextArea recentActivityArea = new JTextArea();
    private final JLabel statusMessageLabel = new JLabel("Ready.");
    private final List<RoleAction> roleActions = new ArrayList<>();
    private JTabbedPane tabs;
    private JPanel boardTab;
    private JPanel boardColumnsPanel;
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
        boardTab = buildBoardTab();
        browseTab = buildBrowseTab();
        newIssueTab = buildNewIssueTab();
        workflowTab = buildWorkflowTab();
        reportsTab = buildReportsTab();
        adminTab = buildAdminTab();
        refreshDeveloperChoices();

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(loginPanel, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(statusMessageLabel, BorderLayout.SOUTH);
        setContentPane(root);
        updateLoginState();
    }

    private JPanel buildBrowseTab() {
        JPanel ticketListPanel = new JPanel(new BorderLayout(6, 6));
        ticketListPanel.setBorder(BorderFactory.createTitledBorder("Issues"));
        configureIssueTable();
        ticketListPanel.add(new JScrollPane(issueTable), BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
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
        JTextArea initialCommentField = new JTextArea(5, 40);
        initialCommentField.setLineWrap(true);
        initialCommentField.setWrapStyleWord(true);
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

        JPanel textFields = new JPanel(new GridLayout(2, 1, 8, 8));
        JPanel descriptionPanel = new JPanel(new BorderLayout(6, 6));
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("Description"));
        descriptionPanel.add(new JScrollPane(descriptionField), BorderLayout.CENTER);
        JPanel initialCommentPanel = new JPanel(new BorderLayout(6, 6));
        initialCommentPanel.setBorder(BorderFactory.createTitledBorder("Initial Comment"));
        initialCommentPanel.add(new JScrollPane(initialCommentField), BorderLayout.CENTER);
        textFields.add(descriptionPanel);
        textFields.add(initialCommentPanel);
        form.add(textFields, BorderLayout.CENTER);

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
            Issue issue = controller.createIssue(selectedProject.getId(), title, description, currentUser.getUsername(), (Priority) priorityBox.getSelectedItem());
            String initialComment = initialCommentField.getText().trim();
            if (!initialComment.isBlank()) {
                controller.addComment(issue.getId(), currentUser.getUsername(), initialComment);
            }
            titleField.setText("");
            descriptionField.setText("");
            initialCommentField.setText("");
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
        configureReadOnly(recommendationArea);
        configureReadOnly(developerArea);
        recommendationArea.setText("Select Assign to view recommended assignees.");
        developerArea.setText("Developer accounts will appear here.");
        workflowApplyButton.setEnabled(false);
        workflowApplyButton.addActionListener(e -> runSafely(() -> {
            if (selectedWorkflowAction == null) {
                throw new IllegalStateException("Choose an action first.");
            }
            selectedWorkflowAction.run();
        }));

        JPanel assigneeRow = new JPanel(new BorderLayout(8, 0));
        assigneeRow.add(workflowAssigneeLabel, BorderLayout.WEST);
        assigneeRow.add(workflowAssigneeBox, BorderLayout.CENTER);

        JPanel commentPanel = new JPanel(new BorderLayout(8, 8));
        commentPanel.add(workflowCommentLabel, BorderLayout.NORTH);
        commentPanel.add(new JScrollPane(workflowCommentArea), BorderLayout.CENTER);

        JPanel recommendationPanel = new JPanel(new BorderLayout());
        recommendationPanel.setBorder(BorderFactory.createTitledBorder("Recommendation Result"));
        recommendationPanel.add(new JScrollPane(recommendationArea), BorderLayout.CENTER);

        JPanel developerPanel = new JPanel(new BorderLayout());
        developerPanel.setBorder(BorderFactory.createTitledBorder("Available Developers"));
        developerPanel.add(new JScrollPane(developerArea), BorderLayout.CENTER);

        JSplitPane assignmentSupportSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, recommendationPanel, developerPanel);
        assignmentSupportSplit.setResizeWeight(0.55);
        assignmentSupportPanel.add(assignmentSupportSplit, BorderLayout.CENTER);
        assignmentSupportPanel.setVisible(false);

        JPanel inputBody = new JPanel(new BorderLayout(8, 8));
        inputBody.add(assignmentSupportPanel, BorderLayout.NORTH);
        inputBody.add(commentPanel, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(workflowModeLabel, BorderLayout.NORTH);
        topPanel.add(assigneeRow, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Action Input"));
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(inputBody, BorderLayout.CENTER);
        panel.add(workflowApplyButton, BorderLayout.SOUTH);
        showWorkflowAssigneeInput(false);
        return panel;
    }

    private JPanel buildReportsTab() {
        configureReadOnly(statsArea);
        configureReadOnly(recentActivityArea);
        statsArea.setText("Dashboard summary will appear here.");
        recentActivityArea.setText("Recent activity will appear here.");

        statusBreakdownPanel.setLayout(new BoxLayout(statusBreakdownPanel, BoxLayout.Y_AXIS));
        priorityBreakdownPanel.setLayout(new BoxLayout(priorityBreakdownPanel, BoxLayout.Y_AXIS));
        workloadPanel.setLayout(new BoxLayout(workloadPanel, BoxLayout.Y_AXIS));

        JPanel summaryWrapper = new JPanel(new BorderLayout(6, 6));
        summaryWrapper.setBorder(BorderFactory.createTitledBorder("Summary"));
        summaryWrapper.add(reportSummaryPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status Breakdown"));
        statusPanel.add(statusBreakdownPanel, BorderLayout.CENTER);

        JPanel priorityPanel = new JPanel(new BorderLayout());
        priorityPanel.setBorder(BorderFactory.createTitledBorder("Priority Breakdown"));
        priorityPanel.add(priorityBreakdownPanel, BorderLayout.CENTER);

        JPanel workloadWrapper = new JPanel(new BorderLayout());
        workloadWrapper.setBorder(BorderFactory.createTitledBorder("Assignee Workload"));
        workloadWrapper.add(workloadPanel, BorderLayout.CENTER);

        JPanel breakdowns = new JPanel(new GridLayout(1, 3, 8, 0));
        breakdowns.add(statusPanel);
        breakdowns.add(priorityPanel);
        breakdowns.add(workloadWrapper);

        JPanel trendPanel = new JPanel(new BorderLayout());
        trendPanel.setBorder(BorderFactory.createTitledBorder("Monthly Issue Trend"));
        trendPanel.add(trendChartPanel, BorderLayout.CENTER);

        JPanel recentPanel = new JPanel(new BorderLayout());
        recentPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));
        recentPanel.add(new JScrollPane(recentActivityArea), BorderLayout.CENTER);

        JSplitPane activitySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, trendPanel, recentPanel);
        activitySplit.setResizeWeight(0.78);

        JSplitPane dashboardBody = new JSplitPane(JSplitPane.VERTICAL_SPLIT, breakdowns, activitySplit);
        dashboardBody.setResizeWeight(0.30);

        JPanel dashboard = new JPanel(new BorderLayout(8, 8));
        dashboard.add(summaryWrapper, BorderLayout.NORTH);
        dashboard.add(dashboardBody, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(dashboard, BorderLayout.CENTER);
        refreshReportDashboard();
        return panel;
    }

    private JPanel buildAdminTab() {
        JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        adminActions.setBorder(BorderFactory.createTitledBorder("Admin"));
        addRoleButton(adminActions, "Add User", this::addUser, Role.ADMIN);
        addRoleButton(adminActions, "Delete User", this::deleteUser, Role.ADMIN);
        addRoleButton(adminActions, "Add Project", this::addProject, Role.ADMIN);
        addRoleButton(adminActions, "Delete Project", this::deleteProject, Role.ADMIN);

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
        detailPanel.setBorder(BorderFactory.createTitledBorder("Issue Detail"));
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
        tabs.addTab("Board", boardTab);
        tabs.addTab("Browse", browseTab);
        if (currentUser != null && currentUser.getRole() == Role.TESTER) {
            tabs.addTab("New Issue", newIssueTab);
        }
        if (currentUser != null && hasAnyActionFor(workflowTab, currentUser.getRole())) {
            tabs.addTab("Issue Actions", workflowTab);
        }
        if (currentUser != null && (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.PL)) {
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
        String selectedPriority = priorityFilterBox.getSelectedItem() == null ? "ALL" : priorityFilterBox.getSelectedItem().toString();
        Priority priority = "ALL".equals(selectedPriority) ? null : Priority.valueOf(selectedPriority);
        List<Issue> results = controller.search(queryField.getText(), reporterField.getText(), assigneeField.getText(), status);
        if (priority != null) {
            results = results.stream()
                    .filter(issue -> issue.getPriority() == priority)
                    .toList();
        }
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

    private JPanel buildBoardTab() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Board");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));

        initializeSearchControls();

        queryField.setPreferredSize(new Dimension(180, 30));
        JButton searchButton = new JButton("Search");
        searchButton.setFocusable(false);
        searchButton.addActionListener(e -> runSafely(this::searchBoard));
        queryField.addActionListener(e -> runSafely(this::searchBoard));

        JComboBox<String> quickFilters = new JComboBox<>(new String[]{
                "Quick filters",
                "All",
                "Open",
                "New",
                "Assigned to Me",
                "Reported by Me",
                "Fixed",
                "Resolved",
                "Closed"
        });
        quickFilters.addActionListener(e -> runSafely(() ->
                applyBoardQuickFilter((String) quickFilters.getSelectedItem())));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(queryField);
        toolbar.add(searchButton);
        toolbar.add(quickFilters);

        JPanel header = new JPanel(new BorderLayout(8, 8));
        header.add(title, BorderLayout.NORTH);
        header.add(toolbar, BorderLayout.SOUTH);

        boardColumnsPanel = new BoardColumnsPanel();
        boardColumnsPanel.setLayout(new GridLayout(1, 4, 8, 0));
        boardColumnsPanel.setBackground(new Color(244, 246, 248));
        boardColumnsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(header, BorderLayout.NORTH);
        JScrollPane boardScrollPane = new JScrollPane(boardColumnsPanel);
        boardScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        boardScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 219, 226)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        boardScrollPane.getViewport().setBackground(new Color(244, 246, 248));
        boardScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateBoardColumnLayout(boardScrollPane.getViewport().getWidth());
            }
        });
        panel.add(boardScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void updateBoardColumnLayout(int viewportWidth) {
        if (boardColumnsPanel == null) {
            return;
        }
        int rows = viewportWidth < 980 ? 2 : 1;
        int columns = rows == 1 ? 4 : 2;
        GridLayout current = (GridLayout) boardColumnsPanel.getLayout();
        if (current.getRows() != rows || current.getColumns() != columns) {
            boardColumnsPanel.setLayout(new GridLayout(rows, columns, 8, 8));
            boardColumnsPanel.revalidate();
        }
    }

    private void initializeSearchControls() {
        if (statusBox.getItemCount() == 0) {
            statusBox.addItem("ALL");
            for (IssueStatus status : IssueStatus.values()) {
                statusBox.addItem(status.name());
            }
        }
        if (priorityFilterBox.getItemCount() == 0) {
            priorityFilterBox.addItem("ALL");
            for (Priority priority : Priority.values()) {
                priorityFilterBox.addItem(priority.name());
            }
        }
    }

    private void addBoardFilterButton(JPanel panel, String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusable(false);
        button.addActionListener(e -> runSafely(action));
        panel.add(button);
    }

    private void searchBoard() {
        String term = queryField.getText() == null ? "" : queryField.getText().trim().toLowerCase();
        if (term.isBlank()) {
            refreshIssues(controller.issues());
            return;
        }
        refreshIssues(controller.issues().stream()
                .filter(issue -> containsIgnoreCase(issue.getTitle(), term)
                        || containsIgnoreCase(issue.getDescription(), term)
                        || containsIgnoreCase(issue.getReporter(), term)
                        || containsIgnoreCase(issue.getAssignee(), term)
                        || containsIgnoreCase(projectNameOf(issue), term)
                        || String.valueOf(issue.getId()).equals(term))
                .toList());
    }

    private void applyBoardQuickFilter(String filter) {
        if (filter == null || "Quick filters".equals(filter)) {
            return;
        }
        queryField.setText("");
        switch (filter) {
            case "All" -> refreshIssues(controller.issues());
            case "Open" -> applyOpenFilter();
            case "New" -> refreshIssues(controller.search("", "", "", IssueStatus.NEW));
            case "Assigned to Me" -> {
                requireLogin();
                refreshIssues(controller.search("", "", currentUser.getUsername(), null));
            }
            case "Reported by Me" -> {
                requireLogin();
                refreshIssues(controller.search("", currentUser.getUsername(), "", null));
            }
            case "Fixed" -> refreshIssues(controller.search("", "", "", IssueStatus.FIXED));
            case "Resolved" -> refreshIssues(controller.search("", "", "", IssueStatus.RESOLVED));
            case "Closed" -> refreshIssues(controller.search("", "", "", IssueStatus.CLOSED));
            default -> refreshIssues(controller.issues());
        }
    }

    private void applyQuickFilter(IssueStatus status, String reporter, String assignee) {
        projectFilterField.setText("");
        queryField.setText("");
        reporterField.setText(reporter == null ? "" : reporter);
        assigneeField.setText(assignee == null ? "" : assignee);
        statusBox.setSelectedItem(status == null ? "ALL" : status.name());
        priorityFilterBox.setSelectedItem("ALL");
        search();
    }

    private void applyOpenFilter() {
        projectFilterField.setText("");
        queryField.setText("");
        reporterField.setText("");
        assigneeField.setText("");
        statusBox.setSelectedItem("ALL");
        priorityFilterBox.setSelectedItem("ALL");
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
        priorityFilterBox.setSelectedItem("ALL");
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
            refreshDeveloperChoices();
            showStatus("User added. Default password is 1234.");
        }
    }

    private void deleteUser() {
        requireRole(Role.ADMIN, "Only admin can delete users.");
        List<UserAccount> users = userController.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException("No user exists.");
        }
        UserAccount user = (UserAccount) JOptionPane.showInputDialog(this, "User", "Delete User",
                JOptionPane.WARNING_MESSAGE, null, users.toArray(), users.get(0));
        if (user == null) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
                "Delete user '" + user.getUsername() + "'?",
                "Confirm Delete User",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }
        userController.delete(currentUser.getUsername(), user.getUsername());
        refreshDeveloperChoices();
        showStatus("User deleted: " + user.getUsername());
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

    private void deleteProject() {
        requireRole(Role.ADMIN, "Only admin can delete projects.");
        List<Project> projects = projectController.findAllProjects();
        if (projects.isEmpty()) {
            throw new IllegalStateException("No project exists.");
        }
        Project project = (Project) JOptionPane.showInputDialog(this, "Project", "Delete Project",
                JOptionPane.WARNING_MESSAGE, null, projects.toArray(), projects.get(0));
        if (project == null) {
            return;
        }
        int option = JOptionPane.showConfirmDialog(this,
                "Delete project '" + project.getName() + "' and all issues in it?",
                "Confirm Delete Project",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }
        projectController.deleteProject(project.getId());
        refreshProjectChoices();
        refreshIssues(controller.issues());
        showStatus("Project deleted: " + project.getName());
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
            updateAssignmentRecommendation(issue);
        }
    }

    private void updateAssignmentRecommendation(Issue issue) {
        refreshDeveloperChoices();
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

    private void stats() {
        refreshReportDashboard();
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

    private void refreshReportDashboard() {
        List<Issue> issues = controller.issues();
        IssueStatistics statistics = controller.statistics();
        long total = issues.size();
        long resolved = countStatus(issues, IssueStatus.RESOLVED);
        long closed = countStatus(issues, IssueStatus.CLOSED);
        long open = issues.stream()
                .filter(issue -> issue.getStatus() != IssueStatus.RESOLVED && issue.getStatus() != IssueStatus.CLOSED)
                .count();
        long unassigned = issues.stream()
                .filter(issue -> issue.getAssignee() == null || issue.getAssignee().isBlank())
                .count();

        reportSummaryPanel.removeAll();
        reportSummaryPanel.add(summaryCard("Total", total, new Color(38, 70, 118)));
        reportSummaryPanel.add(summaryCard("Open", open, new Color(0, 82, 204)));
        reportSummaryPanel.add(summaryCard("Resolved", resolved, new Color(82, 67, 170)));
        reportSummaryPanel.add(summaryCard("Closed", closed, new Color(54, 143, 78)));
        reportSummaryPanel.add(summaryCard("Unassigned", unassigned, new Color(191, 87, 0)));

        statusBreakdownPanel.removeAll();
        long maxStatus = Math.max(1L, statistics.getStatusCounts().values().stream().mapToLong(Long::longValue).max().orElse(1L));
        for (IssueStatus status : IssueStatus.values()) {
            long count = statistics.getStatusCounts().getOrDefault(status, 0L);
            statusBreakdownPanel.add(metricRow(status.name(), count, maxStatus, statusColor(status)));
        }

        priorityBreakdownPanel.removeAll();
        long maxPriority = Math.max(1L, statistics.getPriorityCounts().values().stream().mapToLong(Long::longValue).max().orElse(1L));
        for (Priority priority : Priority.values()) {
            long count = statistics.getPriorityCounts().getOrDefault(priority, 0L);
            priorityBreakdownPanel.add(metricRow(priority.name(), count, maxPriority, priorityColor(priority)));
        }

        workloadPanel.removeAll();
        List<UserAccount> developers = userController.findByRole(Role.DEV);
        long maxWorkload = Math.max(1L, developers.stream()
                .mapToLong(developer -> statistics.getAssigneeCounts().getOrDefault(developer.getUsername(), 0L))
                .max()
                .orElse(1L));
        if (developers.isEmpty()) {
            workloadPanel.add(new JLabel("No developer account exists."));
        } else {
            for (UserAccount developer : developers) {
                long count = statistics.getAssigneeCounts().getOrDefault(developer.getUsername(), 0L);
                workloadPanel.add(metricRow(developer.getUsername(), count, maxWorkload, new Color(0, 82, 204)));
            }
        }

        trendChartPanel.setData(monthlyCreatedCounts(issues), monthlyResolvedCounts(issues));
        recentActivityArea.setText(recentActivityText(issues));
        recentActivityArea.setCaretPosition(0);

        reportSummaryPanel.revalidate();
        statusBreakdownPanel.revalidate();
        priorityBreakdownPanel.revalidate();
        workloadPanel.revalidate();
        trendChartPanel.revalidate();
        reportSummaryPanel.repaint();
        statusBreakdownPanel.repaint();
        priorityBreakdownPanel.repaint();
        workloadPanel.repaint();
        trendChartPanel.repaint();
    }

    private JPanel summaryCard(String label, long value, Color color) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 219, 226)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel valueLabel = new JLabel(String.valueOf(value));
        valueLabel.setForeground(color);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 26f));
        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(new Color(88, 103, 125));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(nameLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel metricRow(String label, long count, long max, Color color) {
        JPanel row = new JPanel(new BorderLayout(8, 4));
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        row.setBackground(Color.WHITE);

        JLabel name = new JLabel(label);
        name.setPreferredSize(new Dimension(96, 22));
        name.setForeground(new Color(40, 57, 86));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 11f));

        JPanel track = new JPanel(new BorderLayout());
        track.setBackground(new Color(235, 238, 242));
        int width = (int) Math.max(4, Math.round((count * 140.0) / max));
        JPanel fill = new JPanel();
        fill.setPreferredSize(new Dimension(width, 12));
        fill.setBackground(color);
        track.add(fill, BorderLayout.WEST);

        JLabel value = new JLabel(String.valueOf(count));
        value.setPreferredSize(new Dimension(34, 22));
        value.setForeground(new Color(88, 103, 125));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 11f));

        row.add(name, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private static long countStatus(List<Issue> issues, IssueStatus status) {
        return issues.stream().filter(issue -> issue.getStatus() == status).count();
    }

    private Map<YearMonth, Long> monthlyCreatedCounts(List<Issue> issues) {
        LinkedHashMap<YearMonth, Long> counts = emptyMonthWindow();
        for (Issue issue : issues) {
            YearMonth month = YearMonth.from(issue.getReportedDate());
            if (counts.containsKey(month)) {
                counts.put(month, counts.get(month) + 1);
            }
        }
        return counts;
    }

    private Map<YearMonth, Long> monthlyResolvedCounts(List<Issue> issues) {
        LinkedHashMap<YearMonth, Long> counts = emptyMonthWindow();
        for (Issue issue : issues) {
            if (issue.getStatus() == IssueStatus.RESOLVED || issue.getStatus() == IssueStatus.CLOSED) {
                YearMonth month = YearMonth.from(latestActivityTime(issue));
                if (counts.containsKey(month)) {
                    counts.put(month, counts.get(month) + 1);
                }
            }
        }
        return counts;
    }

    private LinkedHashMap<YearMonth, Long> emptyMonthWindow() {
        YearMonth current = YearMonth.now().minusMonths(5);
        LinkedHashMap<YearMonth, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            counts.put(current.plusMonths(i), 0L);
        }
        return counts;
    }

    private String recentActivityText(List<Issue> issues) {
        List<ActivityItem> activity = new ArrayList<>();
        for (Issue issue : issues) {
            activity.add(new ActivityItem(issue.getReportedDate(),
                    issue.getReporter() + " created Issue #" + issue.getId() + " " + issue.getTitle()));
            for (IssueComment comment : issue.getComments()) {
                activity.add(new ActivityItem(comment.getCreatedAt(),
                        comment.getAuthorUsername() + " commented on Issue #" + issue.getId() + " " + issue.getTitle()));
            }
        }
        activity.sort(Comparator.comparing(ActivityItem::time).reversed());
        if (activity.isEmpty()) {
            return "No recent activity.";
        }
        StringBuilder builder = new StringBuilder();
        activity.stream().limit(7).forEach(item -> builder
                .append(item.time().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")))
                .append("  ")
                .append(item.text())
                .append('\n'));
        return builder.toString();
    }

    private void refreshIssues(List<Issue> issues) {
        Long previousSelection = selectedIssueId;
        visibleIssues.clear();
        visibleIssues.addAll(issues);
        refreshBoard(issues);
        refreshReportDashboard();
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

    private void refreshBoard(List<Issue> issues) {
        if (boardColumnsPanel == null) {
            return;
        }
        boardColumnsPanel.removeAll();
        boardColumnsPanel.add(buildBoardColumn("TO DO", issues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.NEW || issue.getStatus() == IssueStatus.REOPENED)
                .toList(), new Color(235, 238, 242)));
        boardColumnsPanel.add(buildBoardColumn("IN PROGRESS", issues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.ASSIGNED)
                .toList(), new Color(235, 238, 242)));
        boardColumnsPanel.add(buildBoardColumn("IN REVIEW", issues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.FIXED || issue.getStatus() == IssueStatus.RESOLVED)
                .toList(), new Color(235, 238, 242)));
        boardColumnsPanel.add(buildBoardColumn("DONE", issues.stream()
                .filter(issue -> issue.getStatus() == IssueStatus.CLOSED)
                .toList(), new Color(235, 238, 242)));
        boardColumnsPanel.revalidate();
        boardColumnsPanel.repaint();
    }

    private JPanel buildBoardColumn(String title, List<Issue> issues, Color headerColor) {
        JPanel column = new JPanel(new BorderLayout(0, 8));
        column.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        column.setBackground(new Color(244, 246, 248));

        JLabel header = new JLabel(title + " " + issues.size());
        header.setOpaque(true);
        header.setBackground(headerColor);
        header.setForeground(isDark(headerColor) ? Color.WHITE : new Color(88, 103, 125));
        header.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        column.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel();
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setBackground(new Color(244, 246, 248));
        cards.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        for (Issue issue : issues) {
            JPanel card = buildIssueCard(issue);
            JPanel cardWrapper = new JPanel(new BorderLayout());
            cardWrapper.setBackground(new Color(244, 246, 248));
            cardWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            cardWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardWrapper.add(card, BorderLayout.CENTER);
            cards.add(cardWrapper);
            cards.add(javax.swing.Box.createVerticalStrut(8));
        }
        JScrollPane cardScrollPane = new JScrollPane(cards);
        cardScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cardScrollPane.setBorder(BorderFactory.createEmptyBorder());
        cardScrollPane.getViewport().setBackground(new Color(244, 246, 248));
        column.add(cardScrollPane, BorderLayout.CENTER);
        return column;
    }

    private JPanel buildIssueCard(Issue issue) {
        JPanel card = new JPanel(new BorderLayout(6, 8));
        card.setBackground(Color.WHITE);
        boolean selected = selectedIssueId != null && selectedIssueId == issue.getId();
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? new Color(214, 70, 60) : new Color(214, 219, 226), selected ? 3 : 1),
                BorderFactory.createEmptyBorder(12, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel title = new JLabel("<html><body>" + escapeHtml(issue.getTitle()) + "</body></html>");
        title.setForeground(new Color(40, 57, 86));
        title.setFont(title.getFont().deriveFont(Font.PLAIN, 14f));

        JLabel project = new JLabel(projectNameOf(issue).toUpperCase());
        project.setOpaque(true);
        project.setBackground(priorityColor(issue.getPriority()));
        project.setForeground(Color.WHITE);
        project.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        project.setFont(project.getFont().deriveFont(Font.BOLD, 11f));

        JLabel footer = new JLabel("#" + issue.getId() + "   " + issue.getStatus()
                + "   assignee: " + value(issue.getAssignee()));
        footer.setForeground(new Color(122, 137, 160));
        footer.setFont(footer.getFont().deriveFont(Font.BOLD, 11f));

        card.add(title, BorderLayout.NORTH);
        card.add(project, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        card.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                openIssueFromBoard(issue);
            }
        });
        return card;
    }

    private void openIssueFromBoard(Issue issue) {
        selectedIssueId = issue.getId();
        selectIssueInTable(issue.getId());
        showSelectedIssue();
        refreshBoard(visibleIssues);
        showStatus("Selected issue #" + issue.getId() + ".");
    }

    private void selectIssueInTable(long issueId) {
        for (int i = 0; i < visibleIssues.size(); i++) {
            if (visibleIssues.get(i).getId() == issueId) {
                int viewRow = issueTable.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    issueTable.setRowSelectionInterval(viewRow, viewRow);
                }
                return;
            }
        }
    }

    private void showSelectedIssue() {
        Issue issue = selectedIssueFromTable();
        if (issue == null) {
            return;
        }
        selectedIssueId = issue.getId();
        updateActionVisibility();
        selectedTicketLabel.setText("Selected issue: #" + issue.getId() + " " + issue.getTitle());
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
        selectedTicketLabel.setText("Selected issue: none");
        ticketTitleLabel.setText("No issue selected");
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
        Object selected = workflowAssigneeBox.getSelectedItem();
        return selected == null ? "" : selected.toString().trim();
    }

    private String workflowComment() {
        return workflowCommentArea.getText().trim();
    }

    private void clearWorkflowInputs() {
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
            workflowAssigneeBox.setSelectedIndex(workflowAssigneeBox.getItemCount() == 0 ? -1 : 0);
            assignmentSupportPanel.setVisible(false);
        } else {
            assignmentSupportPanel.setVisible(true);
            Issue issue = selectedIssue();
            if (issue != null) {
                updateAssignmentRecommendation(issue);
            }
        }
        revalidate();
        repaint();
    }

    private void resetWorkflowAction() {
        selectedWorkflowAction = null;
        workflowModeLabel.setText("Choose an action.");
        workflowCommentLabel.setText("Comment");
        workflowApplyButton.setText("Apply");
        workflowApplyButton.setEnabled(false);
        showWorkflowAssigneeInput(false);
        assignmentSupportPanel.setVisible(false);
        clearWorkflowInputs();
    }

    private void showWorkflowAssigneeInput(boolean visible) {
        workflowAssigneeLabel.setVisible(visible);
        workflowAssigneeBox.setVisible(visible);
    }

    private void refreshDeveloperChoices() {
        Object selected = workflowAssigneeBox.getSelectedItem();
        workflowAssigneeBox.removeAllItems();
        List<UserAccount> developers = userController.findByRole(Role.DEV);
        for (UserAccount developer : developers) {
            workflowAssigneeBox.addItem(developer.getUsername());
        }
        if (selected != null) {
            workflowAssigneeBox.setSelectedItem(selected);
        }
        updateDeveloperArea(developers);
    }

    private void updateDeveloperArea(List<UserAccount> developers) {
        if (developerArea == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        if (developers.isEmpty()) {
            builder.append("No developer account exists.");
        } else {
            builder.append("Developers\n");
            builder.append(String.format("%-16s %s%n", "Username", "Assigned"));
            builder.append("--------------------------\n");
            for (UserAccount developer : developers) {
                long assignedCount = controller.statistics()
                        .getAssigneeCounts()
                        .getOrDefault(developer.getUsername(), 0L);
                builder.append(String.format("%-16s %d%n", developer.getUsername(), assignedCount));
            }
        }
        developerArea.setText(builder.toString());
        developerArea.setCaretPosition(0);
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

    private static boolean containsIgnoreCase(String value, String normalizedTerm) {
        return value != null && value.toLowerCase().contains(normalizedTerm);
    }

    private static Color priorityColor(Priority priority) {
        return switch (priority) {
            case BLOCKER -> new Color(126, 87, 194);
            case CRITICAL -> new Color(213, 67, 55);
            case MAJOR -> new Color(244, 132, 18);
            case MINOR -> new Color(69, 184, 205);
            case TRIVIAL -> new Color(93, 173, 99);
        };
    }

    private static Color statusColor(IssueStatus status) {
        return switch (status) {
            case NEW -> new Color(122, 137, 160);
            case ASSIGNED -> new Color(0, 82, 204);
            case FIXED -> new Color(82, 67, 170);
            case RESOLVED -> new Color(54, 143, 78);
            case CLOSED -> new Color(37, 56, 88);
            case REOPENED -> new Color(191, 87, 0);
        };
    }

    private static boolean isDark(Color color) {
        int brightness = (color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114) / 1000;
        return brightness < 150;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String projectNameOf(Issue issue) {
        return projectController.findAllProjects().stream()
                .filter(project -> project.getId() == issue.getProjectId())
                .map(Project::getName)
                .findFirst()
                .orElse(String.valueOf(issue.getProjectId()));
    }

    private static String latestActivity(Issue issue) {
        return latestActivityTime(issue).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private static LocalDateTime latestActivityTime(Issue issue) {
        if (issue.getComments().isEmpty()) {
            return issue.getReportedDate();
        }
        return issue.getComments().get(issue.getComments().size() - 1).getCreatedAt();
    }

    private record ActivityItem(LocalDateTime time, String text) {
    }

    private static class BoardColumnsPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 24, 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static class TrendChartPanel extends JPanel {
        private Map<YearMonth, Long> created = Map.of();
        private Map<YearMonth, Long> resolved = Map.of();

        private TrendChartPanel() {
            setPreferredSize(new Dimension(520, 190));
            setBackground(Color.WHITE);
        }

        private void setData(Map<YearMonth, Long> created, Map<YearMonth, Long> resolved) {
            this.created = new LinkedHashMap<>(created);
            this.resolved = new LinkedHashMap<>(resolved);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int left = 42;
            int top = 24;
            int right = getWidth() - 24;
            int bottom = getHeight() - 34;
            g.setColor(new Color(235, 238, 242));
            for (int i = 0; i <= 4; i++) {
                int y = top + ((bottom - top) * i / 4);
                g.drawLine(left, y, right, y);
            }

            long max = Math.max(
                    created.values().stream().mapToLong(Long::longValue).max().orElse(1L),
                    resolved.values().stream().mapToLong(Long::longValue).max().orElse(1L));
            max = Math.max(max, 1L);

            drawSeries(g, created, left, top, right, bottom, max, new Color(0, 82, 204));
            drawSeries(g, resolved, left, top, right, bottom, max, new Color(191, 87, 0));

            List<YearMonth> months = new ArrayList<>(created.keySet());
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            g.setColor(new Color(88, 103, 125));
            int step = months.size() <= 1 ? 0 : (right - left) / (months.size() - 1);
            for (int i = 0; i < months.size(); i++) {
                String label = months.get(i).getMonth().toString().substring(0, 3);
                g.drawString(label, left + (step * i) - 10, getHeight() - 12);
            }

            g.setColor(new Color(0, 82, 204));
            g.fillOval(right - 130, 8, 7, 7);
            g.setColor(new Color(40, 57, 86));
            g.drawString("Created", right - 118, 15);
            g.setColor(new Color(191, 87, 0));
            g.fillOval(right - 66, 8, 7, 7);
            g.setColor(new Color(40, 57, 86));
            g.drawString("Resolved", right - 54, 15);
            g.dispose();
        }

        private void drawSeries(Graphics2D g, Map<YearMonth, Long> values, int left, int top,
                                int right, int bottom, long max, Color color) {
            List<Long> points = new ArrayList<>(values.values());
            if (points.isEmpty()) {
                return;
            }
            int step = points.size() <= 1 ? 0 : (right - left) / (points.size() - 1);
            g.setColor(color);
            int previousX = -1;
            int previousY = -1;
            for (int i = 0; i < points.size(); i++) {
                int x = left + (step * i);
                int y = bottom - (int) Math.round((points.get(i) * (bottom - top)) / (double) max);
                g.fillOval(x - 3, y - 3, 6, 6);
                if (previousX >= 0) {
                    g.drawLine(previousX, previousY, x, y);
                }
                previousX = x;
                previousY = y;
            }
        }
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
