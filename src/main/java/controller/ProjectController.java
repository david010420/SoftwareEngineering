package controller;

import java.util.List;
import java.util.Objects;

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
}
