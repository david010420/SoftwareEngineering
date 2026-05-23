package repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import model.Project;

public class InMemoryProjectRepository implements ProjectRepository {
    private final Map<Long, Project> projects = new LinkedHashMap<>();
    private long nextProjectId = 1L;

    @Override
    public void initialize() {
    }

    @Override
    public Project save(Project project) {
        if (project.getId() == 0L) {
            project.setId(nextProjectId++);
        }
        projects.put(project.getId(), project);
        return project;
    }

    @Override
    public Optional<Project> findById(long id) {
        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public List<Project> findAll() {
        return new ArrayList<>(projects.values());
    }
}
