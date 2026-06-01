package controller;

import java.util.List;
import java.util.Objects;

import model.Issue;
import model.Priority;
import model.Project;
import service.ProjectService;

public class ProjectController {
    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    public Project createProject(String name) {
        return service.createProject(name);
    }

    public Project getProject(long id) {
        return service.getProject(id);
    }

    public List<Project> findAllProjects() {
        return service.findAllProjects();
    }

    public Project findByName(String name) {
        return service.findByName(name);
    }

    public Issue addIssue(long projectId, String title, String description, String reporterUsername, Priority priority) {
        return service.addIssue(projectId, title, description, reporterUsername, priority);
    }

    public void deleteProject(long projectId) {
        service.deleteProject(projectId);
    }
}
