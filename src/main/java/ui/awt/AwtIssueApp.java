package its.ui.awt;

import app.AppFactory;
import controller.IssueController;
import controller.ProjectController;
import controller.UserController;
import model.Issue;
import model.IssueStatus;
import model.Priority;
import model.Project;
import model.Role;
import model.UserAccount;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class AwtIssueApp extends Frame {
    private final IssueController controller;
    private final ProjectController projectController;
    private final UserController userController;
    private final List issueList = new List();
    private final TextArea details = new TextArea();
    private final Label currentUserLabel = new Label("Not logged in");
    private final TextField loginUsername = new TextField("tester1");
    private final TextField loginPassword = new TextField("1234");
    private final TextField query = new TextField();
    private final TextField reporter = new TextField();
    private final TextField assignee = new TextField();
    private final Choice status = new Choice();
    private final Choice projectChoice = new Choice();
    private final TextField title = new TextField("AWT created issue");
    private final TextArea description = new TextArea("Created from AWT UI", 4, 60);
    private final Choice priority = new Choice();
    private final Choice actionAssignee = new Choice();
    private final TextArea actionComment = new TextArea("Updated from AWT UI", 3, 40);
    private final TextField adminUsername = new TextField("newUser");
    private final TextField adminPassword = new TextField();
    private final Choice adminRole = new Choice();
    private final TextField adminProject = new TextField("new-project");
    private final TextArea reportOutput = new TextArea();
    private final Label message = new Label("Ready.");
    private final java.util.List<RoleAction> roleActions = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final Panel cardPanel = new Panel(cards);
    private Button browseNav;
    private Button newIssueNav;
    private Button actionsNav;
    private Button reportsNav;
    private Button adminNav;
    private String currentCard = "Browse";
    private java.util.List<Issue> currentIssues;
    private UserAccount currentUser;

    public AwtIssueApp(IssueController controller, ProjectController projectController, UserController userController) {
        super("Issue Management System - AWT");
        this.controller = controller;
        this.projectController = projectController;
        this.userController = userController;
        setSize(1180, 760);
        buildUi();
        refreshProjectChoices();
        refreshDeveloperChoices();
        refresh(controller.issues());
        updateActionVisibility();
    }

    private void buildUi() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        loginPassword.setEchoChar('*');
        Panel loginPanel = new Panel(new GridLayout(1, 7, 6, 6));
        loginPanel.add(new Label("Username"));
        loginPanel.add(loginUsername);
        loginPanel.add(new Label("Password"));
        loginPanel.add(loginPassword);
        addButton(loginPanel, "Login", this::login);
        addButton(loginPanel, "Switch User", this::login);
        loginPanel.add(currentUserLabel);

        Panel navigation = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        browseNav = addButton(navigation, "Browse", () -> showCard("Browse"));
        newIssueNav = addButton(navigation, "New Issue", () -> showCard("New Issue"));
        actionsNav = addButton(navigation, "Issue Actions", () -> showCard("Issue Actions"));
        reportsNav = addButton(navigation, "Reports", () -> showCard("Reports"));
        adminNav = addButton(navigation, "Admin", () -> showCard("Admin"));

        Panel north = new Panel(new BorderLayout(6, 6));
        north.add(loginPanel, BorderLayout.NORTH);
        north.add(navigation, BorderLayout.SOUTH);

        Panel queryPanel = new Panel(new BorderLayout(6, 6));
        Panel top = new Panel(new GridLayout(2, 6, 6, 6));
        status.add("ALL");
        for (IssueStatus value : IssueStatus.values()) {
            status.add(value.name());
        }
        top.add(new Label("Query"));
        top.add(new Label("Reporter"));
        top.add(new Label("Assignee"));
        top.add(new Label("Status"));
        top.add(new Label(""));
        top.add(new Label(""));
        top.add(query);
        top.add(reporter);
        top.add(assignee);
        top.add(status);
        Button search = new Button("Search");
        search.addActionListener(e -> search());
        top.add(search);
        Button reset = new Button("Reset");
        reset.addActionListener(e -> resetSearch());
        top.add(reset);
        queryPanel.add(new Label("Issue Query"), BorderLayout.NORTH);
        queryPanel.add(top, BorderLayout.CENTER);

        issueList.addItemListener(e -> showSelected());
        details.setEditable(false);

        Panel ticketListPanel = new Panel(new BorderLayout(6, 6));
        ticketListPanel.add(new Label("Issues"), BorderLayout.NORTH);
        ticketListPanel.add(issueList, BorderLayout.CENTER);

        Panel detailPanel = new Panel(new BorderLayout(6, 6));
        detailPanel.add(new Label("Issue Detail"), BorderLayout.NORTH);
        detailPanel.add(details, BorderLayout.CENTER);

        Panel center = new Panel(new GridLayout(1, 2, 8, 8));
        center.add(ticketListPanel);
        center.add(detailPanel);

        Panel browse = new Panel(new BorderLayout(8, 8));
        browse.add(queryPanel, BorderLayout.NORTH);
        browse.add(center, BorderLayout.CENTER);

        for (Priority value : Priority.values()) {
            priority.add(value.name());
        }
        priority.select(Priority.MAJOR.name());

        for (Role value : Role.values()) {
            adminRole.add(value.name());
        }
        adminRole.select(Role.DEV.name());

        Panel newIssueFields = formPanel();
        addFormRow(newIssueFields, 0, "Project", projectChoice);
        addFormRow(newIssueFields, 1, "Title", title);
        addFormRow(newIssueFields, 2, "Description", description);
        addFormRow(newIssueFields, 3, "Priority", priority);
        Panel newIssueActions = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        addRoleButton(newIssueActions, "Create Issue", this::newIssue, null, Role.TESTER);

        Panel newIssue = new Panel(new BorderLayout(6, 6));
        newIssue.add(new Label("New Issue"), BorderLayout.NORTH);
        newIssue.add(newIssueFields, BorderLayout.NORTH);
        newIssue.add(newIssueActions, BorderLayout.CENTER);

        Panel actionInput = new Panel(new BorderLayout(6, 6));
        Panel actionInputFields = new Panel(new GridLayout(2, 2, 8, 8));
        actionInputFields.add(new Label("Assignee"));
        actionInputFields.add(new Label("Comment"));
        actionInputFields.add(actionAssignee);
        actionInputFields.add(actionComment);
        actionInput.add(new Label("Action Input"), BorderLayout.NORTH);
        actionInput.add(actionInputFields, BorderLayout.CENTER);

        Panel actionButtons = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        addRoleButton(actionButtons, "Comment", this::comment, new IssueStatus[0], Role.ADMIN, Role.PL, Role.DEV, Role.TESTER);
        addRoleButton(actionButtons, "Assign", this::assign, new IssueStatus[]{IssueStatus.NEW, IssueStatus.REOPENED}, Role.PL);
        addRoleButton(actionButtons, "Fix", this::fix, new IssueStatus[]{IssueStatus.ASSIGNED, IssueStatus.REOPENED}, Role.DEV);
        addRoleButton(actionButtons, "Resolve", () -> change(IssueStatus.RESOLVED), new IssueStatus[]{IssueStatus.FIXED}, Role.TESTER);
        addRoleButton(actionButtons, "Close", () -> change(IssueStatus.CLOSED), new IssueStatus[]{IssueStatus.RESOLVED}, Role.PL);
        addRoleButton(actionButtons, "Reopen", () -> change(IssueStatus.REOPENED), new IssueStatus[]{IssueStatus.RESOLVED, IssueStatus.CLOSED}, Role.TESTER);

        Panel actions = new Panel(new BorderLayout(6, 6));
        actions.add(actionInput, BorderLayout.NORTH);
        actions.add(actionButtons, BorderLayout.CENTER);
        actions.add(new Panel(), BorderLayout.SOUTH);

        Panel reportsButtons = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        addRoleButton(reportsButtons, "Recommend", this::recommend, new IssueStatus[0], Role.PL);
        addRoleButton(reportsButtons, "Refresh Developers", this::refreshDeveloperChoices, null, Role.PL);
        addRoleButton(reportsButtons, "Stats", this::stats, null, Role.ADMIN, Role.PL);
        reportOutput.setEditable(false);
        Panel reports = new Panel(new BorderLayout(6, 6));
        reports.add(new Label("Reports"), BorderLayout.NORTH);
        reports.add(reportsButtons, BorderLayout.NORTH);
        reports.add(reportOutput, BorderLayout.CENTER);

        adminPassword.setEchoChar('*');
        Panel adminFields = formPanel();
        addFormRow(adminFields, 0, "Username", adminUsername);
        addFormRow(adminFields, 1, "Password", adminPassword);
        addFormRow(adminFields, 2, "Role", adminRole);
        addFormRow(adminFields, 3, "Project", adminProject);
        Panel adminButtons = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        addRoleButton(adminButtons, "Add User", this::addUser, null, Role.ADMIN);
        addRoleButton(adminButtons, "Add Project", this::addProject, null, Role.ADMIN);

        Panel admin = new Panel(new BorderLayout(6, 6));
        admin.add(new Label("Admin"), BorderLayout.NORTH);
        admin.add(adminFields, BorderLayout.NORTH);
        admin.add(adminButtons, BorderLayout.CENTER);
        admin.add(new Panel(), BorderLayout.SOUTH);

        cardPanel.add(browse, "Browse");
        cardPanel.add(newIssue, "New Issue");
        cardPanel.add(actions, "Issue Actions");
        cardPanel.add(reports, "Reports");
        cardPanel.add(admin, "Admin");

        add(north, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
    }

    private Button addButton(Panel panel, String text, Runnable action) {
        Button button = new Button(text);
        button.addActionListener(e -> safe(action));
        panel.add(button);
        return button;
    }

    private void addRoleButton(Panel panel, String text, Runnable action, IssueStatus[] statuses, Role... roles) {
        Button button = addButton(panel, text, action);
        roleActions.add(new RoleAction(button, statuses, roles));
    }

    private void showCard(String name) {
        currentCard = name;
        cards.show(cardPanel, name);
        showMessage(name);
    }

    private Panel formPanel() {
        Panel panel = new Panel(new GridBagLayout());
        return panel;
    }

    private void addFormRow(Panel panel, int row, String label, java.awt.Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(8, 8, 8, 12);
        panel.add(new Label(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(8, 0, 8, 8);
        panel.add(field, fieldConstraints);
    }

    private void search() {
        IssueStatus selected = "ALL".equals(status.getSelectedItem()) ? null : IssueStatus.valueOf(status.getSelectedItem());
        refresh(controller.search(query.getText(), reporter.getText(), assignee.getText(), selected));
    }

    private void resetSearch() {
        query.setText("");
        reporter.setText("");
        assignee.setText("");
        status.select("ALL");
        refresh(controller.issues());
        showMessage("Filters reset.");
    }

    private void newIssue() {
        requireRole(Role.TESTER, "Only tester can create issues.");
        Project selectedProject = selectedProject();
        controller.createIssue(selectedProject.getId(), title.getText(), description.getText(), currentUser.getUsername(), Priority.valueOf(priority.getSelectedItem()));
        refresh(controller.issues());
        showMessage("Issue created.");
    }

    private void comment() {
        Issue issue = selected();
        if (issue != null) {
            requireLogin();
            controller.addComment(issue.getId(), currentUser.getUsername(), actionComment.getText());
            refresh(controller.issues());
            showMessage("Comment added.");
        }
    }

    private void assign() {
        Issue issue = selected();
        if (issue != null) {
            requireRole(Role.PL, "Only PL can assign issues.");
            controller.assignIssue(issue.getId(), selectedDeveloper(), currentUser.getUsername(), actionComment.getText());
            refresh(controller.issues());
            showMessage("Issue assigned.");
        }
    }

    private void fix() {
        Issue issue = selected();
        if (issue != null) {
            requireRole(Role.DEV, "Only dev can fix issues.");
            controller.markFixed(issue.getId(), currentUser.getUsername(), actionComment.getText());
            refresh(controller.issues());
            showMessage("Issue fixed.");
        }
    }

    private void change(IssueStatus newStatus) {
        Issue issue = selected();
        if (issue != null) {
            requireLogin();
            if (newStatus == IssueStatus.CLOSED) {
                requireRole(Role.PL, "Only PL can close issues.");
            }
            controller.changeStatus(issue.getId(), newStatus, currentUser.getUsername(), actionComment.getText());
            refresh(controller.issues());
            showMessage("Issue changed to " + newStatus + ".");
        }
    }

    private void recommend() {
        Issue issue = selected();
        if (issue != null) {
            requireRole(Role.PL, "Only PL can recommend assignees.");
            refreshDeveloperChoices();
            String candidates = String.join(", ", controller.recommendAssignees(issue.getId()));
            reportOutput.setText("Selected issue: #" + issue.getId() + " " + issue.getTitle() + "\n"
                    + "Best candidate: " + (candidates.isBlank() ? "No candidate yet." : candidates) + "\n\n"
                    + developerSummary());
        }
    }

    private void stats() {
        requireLogin();
        StringBuilder builder = new StringBuilder("Daily Issue Counts\n");
        for (Map.Entry<java.time.LocalDate, Long> entry : controller.statistics().getDailyCounts().entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        builder.append("\nMonthly Issue Counts\n");
        for (Map.Entry<java.time.YearMonth, Long> entry : controller.statistics().getMonthlyCounts().entrySet()) {
            builder.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        reportOutput.setText(builder.toString());
    }

    private void refresh(java.util.List<Issue> issues) {
        currentIssues = issues;
        issueList.removeAll();
        for (Issue issue : issues) {
            issueList.add("#" + issue.getId() + " [" + issue.getStatus() + "] " + issue.getTitle());
        }
        if (!issues.isEmpty()) {
            issueList.select(0);
            showSelected();
        } else {
            details.setText("");
            updateActionVisibility();
        }
    }

    private void showSelected() {
        Issue issue = selected();
        if (issue == null) {
            return;
        }
        details.setText("#" + issue.getId() + "\n"
                + "Project: " + issue.getProjectId() + "\n"
                + "Title: " + issue.getTitle() + "\n"
                + "Description: " + issue.getDescription() + "\n"
                + "Reporter: " + issue.getReporter() + "\n"
                + "Priority: " + issue.getPriority() + "\n"
                + "Status: " + issue.getStatus() + "\n"
                + "Assignee: " + issue.getAssignee() + "\n"
                + "Fixer: " + issue.getFixer() + "\n"
                + "Reported: " + issue.getReportedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "\n"
                + "Comments: " + issue.getComments().size() + "\n"
                + commentHistory(issue));
        updateActionVisibility();
    }

    private Issue selected() {
        int index = issueList.getSelectedIndex();
        return index < 0 || currentIssues == null || index >= currentIssues.size() ? null : currentIssues.get(index);
    }

    private void safe(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            details.setText(e.getMessage());
            showMessage(e.getMessage());
        }
    }

    private void login() {
        currentUser = userController.login(loginUsername.getText(), loginPassword.getText());
        currentUserLabel.setText("Logged in: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        updateActionVisibility();
        showMessage("Logged in.");
    }

    private void addUser() {
        requireRole(Role.ADMIN, "Only admin can add users.");
        String username = adminUsername.getText().trim();
        String password = adminPassword.getText().trim();
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Username과 Password를 입력해 주세요.");
        }
        userController.register(currentUser.getUsername(), username, password, Role.valueOf(adminRole.getSelectedItem()));
        adminPassword.setText("");
        refreshDeveloperChoices();
        showMessage("User added: " + username);
    }

    private void addProject() {
        requireRole(Role.ADMIN, "Only admin can add projects.");
        projectController.createProject(adminProject.getText());
        refreshProjectChoices();
        showMessage("Project added.");
    }

    private void refreshProjectChoices() {
        String selected = projectChoice.getSelectedItem();
        projectChoice.removeAll();
        for (Project project : projectController.findAllProjects()) {
            projectChoice.add(project.getName());
            if (selected != null && selected.equalsIgnoreCase(project.getName())) {
                projectChoice.select(project.getName());
            }
        }
    }

    private Project selectedProject() {
        int index = projectChoice.getSelectedIndex();
        java.util.List<Project> projects = projectController.findAllProjects();
        if (index < 0 || index >= projects.size()) {
            throw new IllegalStateException("Project is required.");
        }
        return projects.get(index);
    }

    private void refreshDeveloperChoices() {
        String selected = actionAssignee.getSelectedItem();
        actionAssignee.removeAll();
        for (UserAccount developer : userController.findByRole(Role.DEV)) {
            actionAssignee.add(developer.getUsername());
            if (selected != null && selected.equalsIgnoreCase(developer.getUsername())) {
                actionAssignee.select(developer.getUsername());
            }
        }
        reportOutput.setText(developerSummary());
    }

    private String selectedDeveloper() {
        String selected = actionAssignee.getSelectedItem();
        if (selected == null || selected.isBlank()) {
            throw new IllegalStateException("No developer account exists.");
        }
        return selected;
    }

    private String developerSummary() {
        StringBuilder builder = new StringBuilder("Available Developers\n");
        java.util.List<UserAccount> developers = userController.findByRole(Role.DEV);
        if (developers.isEmpty()) {
            builder.append("No developer account exists.");
            return builder.toString();
        }
        Map<String, Long> assigneeCounts = controller.statistics().getAssigneeCounts();
        for (UserAccount developer : developers) {
            builder.append("- ")
                    .append(developer.getUsername())
                    .append(" (assigned: ")
                    .append(assigneeCounts.getOrDefault(developer.getUsername(), 0L))
                    .append(")\n");
        }
        return builder.toString();
    }

    private void updateActionVisibility() {
        Issue issue = selected();
        for (RoleAction action : roleActions) {
            action.button.setEnabled(action.isAvailableFor(currentUser, issue));
        }
        boolean loggedIn = currentUser != null;
        Role role = loggedIn ? currentUser.getRole() : null;
        browseNav.setEnabled(true);
        newIssueNav.setEnabled(role == Role.TESTER);
        actionsNav.setEnabled(role == Role.PL || role == Role.DEV || role == Role.TESTER || role == Role.ADMIN);
        reportsNav.setEnabled(role == Role.PL || role == Role.ADMIN);
        adminNav.setEnabled(role == Role.ADMIN);
        if (!isCurrentCardVisible()) {
            showCard("Browse");
        }
        validate();
        repaint();
    }

    private boolean isCurrentCardVisible() {
        if ("Browse".equals(currentCard)) {
            return true;
        }
        if ("New Issue".equals(currentCard)) {
            return newIssueNav.isEnabled();
        }
        if ("Issue Actions".equals(currentCard)) {
            return actionsNav.isEnabled();
        }
        if ("Reports".equals(currentCard)) {
            return reportsNav.isEnabled();
        }
        if ("Admin".equals(currentCard)) {
            return adminNav.isEnabled();
        }
        return false;
    }

    private void requireLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("Login first.");
        }
    }

    private void requireRole(Role role, String errorMessage) {
        requireLogin();
        if (currentUser.getRole() != role) {
            throw new IllegalStateException(errorMessage);
        }
    }

    private String commentHistory(Issue issue) {
        if (issue.getComments().isEmpty()) {
            return "No comments yet.\n";
        }
        StringBuilder builder = new StringBuilder("\nChange History\n");
        issue.getComments().forEach(comment -> builder.append("[")
                .append(comment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("] ")
                .append(comment.getAuthorUsername())
                .append(": ")
                .append(comment.getBody())
                .append('\n'));
        return builder.toString();
    }

    private void showMessage(String text) {
        message.setText(text == null || text.isBlank() ? "Ready." : text);
    }

    public static void main(String[] args) {
        AppFactory.Controllers controllers = AppFactory.createControllers();
        new AwtIssueApp(
                controllers.issueController(),
                controllers.projectController(),
                controllers.userController()).setVisible(true);
    }

    private static class RoleAction {
        private final Button button;
        private final java.util.List<IssueStatus> statuses;
        private final java.util.List<Role> roles;

        private RoleAction(Button button, IssueStatus[] statuses, Role... roles) {
            this.button = button;
            this.statuses = statuses == null ? null : Arrays.asList(statuses);
            this.roles = Arrays.asList(roles);
        }

        private boolean isAvailableFor(UserAccount user, Issue issue) {
            if (user == null || !roles.contains(user.getRole())) {
                return false;
            }
            if (statuses == null) {
                return true;
            }
            if (issue == null) {
                return false;
            }
            return statuses.isEmpty() || statuses.contains(issue.getStatus());
        }
    }
}
