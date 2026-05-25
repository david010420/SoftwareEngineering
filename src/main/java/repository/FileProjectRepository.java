package repository;

import model.Project;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class FileProjectRepository implements ProjectRepository {
    private final Path path;

    public FileProjectRepository(Path path) {
        this.path = path;
    }

    @Override
    public void initialize() {
        FileStore.save(path, FileStore.load(path));
    }

    @Override
    public Project save(Project project) {
        FileStore store = FileStore.load(path);
        if (project.getId() == 0L) {
            project.setId(nextProjectId(store));
        }
        store.projects().removeIf(existing -> existing.getId() == project.getId());
        store.projects().add(project);
        FileStore.save(path, store);
        return project;
    }

    @Override
    public Optional<Project> findById(long id) {
        return FileStore.load(path).projects().stream()
                .filter(project -> project.getId() == id)
                .findFirst();
    }

    @Override
    public List<Project> findAll() {
        return FileStore.load(path).projects().stream()
                .sorted(Comparator.comparing(Project::getId))
                .toList();
    }

    private long nextProjectId(FileStore store) {
        return store.projects().stream()
                .mapToLong(Project::getId)
                .max()
                .orElse(0L) + 1L;
    }
}
