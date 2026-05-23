package service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return repository.save(Project.create(name));
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

    private Project loadIssues(Project project) {
        if (issueRepository == null) {
            return project;
        }
        project.setIssues(issueRepository.findByProjectId(project.getId()));
        return project;
    }
}
