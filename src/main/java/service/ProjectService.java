package service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import model.Issue;
import model.Priority;
import model.Project;
import repository.IssueRepository;
import repository.ProjectRepository;

public class ProjectService {
    private final ProjectRepository repository;
    private final IssueRepository issueRepository;

    public ProjectService(ProjectRepository repository) {
        this(repository, null);
    }

    public ProjectService(ProjectRepository repository, IssueRepository issueRepository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.issueRepository = issueRepository;
    }

    public Project createProject(String name) {
        String normalizedName = requireText(name, "name");
        boolean exists = repository.findAll().stream()
                .anyMatch(project -> project.getName().equalsIgnoreCase(normalizedName));
        if (exists) {
            throw new IllegalArgumentException("Project already exists: " + normalizedName);
        }
        return repository.save(Project.create(normalizedName));
    }

    public Project getProject(long id) {
        Project project = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown project: " + id));
        return loadIssues(project);
    }

    public List<Project> findAllProjects() {
        return repository.findAll().stream()
                .map(this::loadIssues)
                .collect(Collectors.toList());
    }

    public Project findByName(String name) {
        String normalizedName = requireText(name, "name");
        return repository.findAll().stream()
                .filter(project -> project.getName().equalsIgnoreCase(normalizedName))
                .findFirst()
                .map(this::loadIssues)
                .orElseThrow(() -> new IllegalArgumentException("Unknown project: " + name));
    }

    public Issue addIssue(long projectId, String title, String description, String reporterUsername, Priority priority) {
        requireIssueRepository();
        Project project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown project: " + projectId));
        Issue issue = Issue.report(projectId, title, description, reporterUsername, priority);
        project.addIssue(issue);
        return issueRepository.save(issue);
    }

    public void deleteProject(long projectId) {
        if (!repository.exists(projectId)) {
            throw new IllegalArgumentException("Unknown project: " + projectId);
        }
        requireIssueRepository();
        issueRepository.deleteByProjectId(projectId);
        repository.delete(projectId);
    }

    private void requireIssueRepository() {
        if (issueRepository == null) {
            throw new IllegalStateException("IssueRepository is required for project issue operations.");
        }
    }

    private Project loadIssues(Project project) {
        if (issueRepository == null) {
            return project;
        }
        project.setIssues(issueRepository.findByProjectId(project.getId()));
        return project;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
