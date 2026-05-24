package its.ui.awt;

import its.AppFactory;
import its.controller.IssueController;
import its.model.Issue;
import its.model.IssueStatus;
import its.model.Priority;
import its.service.IssueSearchCriteria;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class AwtIssueApp extends Frame {
    private final IssueController controller;
    private final List issueList = new List();
    private final TextArea details = new TextArea();
    private final TextField query = new TextField();
    private final Choice status = new Choice();
    private java.util.List<Issue> currentIssues;

    public AwtIssueApp(IssueController controller) {
        super("Issue Management System - AWT");
        this.controller = controller;
        setSize(980, 620);
        buildUi();
        refresh(controller.issues());
    }

    private void buildUi() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        Panel top = new Panel(new GridLayout(2, 3, 6, 6));
        status.add("");
        for (IssueStatus value : IssueStatus.values()) {
            status.add(value.name());
        }
        top.add(new Label("Query"));
        top.add(new Label("Status"));
        top.add(new Label(""));
        top.add(query);
        top.add(status);
        Button search = new Button("Search");
        search.addActionListener(e -> search());
        top.add(search);

        issueList.addItemListener(e -> showSelected());
        details.setEditable(false);
        Panel center = new Panel(new GridLayout(1, 2, 8, 8));
        center.add(issueList);
        center.add(details);

        Panel actions = new Panel(new GridLayout(1, 5, 6, 6));
        addButton(actions, "New", this::newIssue);
        addButton(actions, "Assign", this::assign);
        addButton(actions, "Fix", this::fix);
        addButton(actions, "Resolve", () -> change(IssueStatus.RESOLVED));
        addButton(actions, "Recommend", this::recommend);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    private void addButton(Panel panel, String text, Runnable action) {
        Button button = new Button(text);
        button.addActionListener(e -> safe(action));
        panel.add(button);
    }

    private void search() {
        IssueStatus selected = status.getSelectedItem().isBlank() ? null : IssueStatus.valueOf(status.getSelectedItem());
        refresh(controller.search(new IssueSearchCriteria().query(query.getText()).status(selected)));
    }

    private void newIssue() {
        controller.createIssue("project1", "1", "AWT created issue", "Created from AWT UI", "tester1", Priority.MAJOR);
        refresh(controller.issues());
    }

    private void assign() {
        Issue issue = selected();
        if (issue != null) {
            controller.assignIssue(issue.getId(), "dev1", "PL1", "Assigned from AWT UI");
            refresh(controller.issues());
        }
    }

    private void fix() {
        Issue issue = selected();
        if (issue != null) {
            controller.markFixed(issue.getId(), "dev1", "Fixed from AWT UI");
            refresh(controller.issues());
        }
    }

    private void change(IssueStatus newStatus) {
        Issue issue = selected();
        if (issue != null) {
            controller.changeStatus(issue.getId(), newStatus, "tester1", "Status changed from AWT UI");
            refresh(controller.issues());
        }
    }

    private void recommend() {
        Issue issue = selected();
        if (issue != null) {
            details.append("\nBest candidate: " + String.join(", ", controller.recommendAssignees(issue.getId())));
        }
    }

    private void refresh(java.util.List<Issue> issues) {
        currentIssues = issues;
        issueList.removeAll();
        for (Issue issue : issues) {
            issueList.add(issue.toString());
        }
        if (!issues.isEmpty()) {
            issueList.select(0);
            showSelected();
        } else {
            details.setText("");
        }
    }

    private void showSelected() {
        Issue issue = selected();
        if (issue == null) {
            return;
        }
        details.setText("#" + issue.getId() + "\n"
                + "Title: " + issue.getTitle() + "\n"
                + "Description: " + issue.getDescription() + "\n"
                + "Reporter: " + issue.getReporter() + "\n"
                + "Status: " + issue.getStatus() + "\n"
                + "Assignee: " + issue.getAssignee() + "\n"
                + "Fixer: " + issue.getFixer() + "\n"
                + "Comments: " + issue.getComments().size() + "\n");
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
        }
    }

    public static void main(String[] args) {
        new AwtIssueApp(AppFactory.createController()).setVisible(true);
    }
}
