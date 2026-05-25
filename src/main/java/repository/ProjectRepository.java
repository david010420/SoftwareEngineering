package repository;

import java.util.List;
import java.util.Optional;

import model.Project;

public interface ProjectRepository {
    void initialize();

    Project save(Project project);

    Optional<Project> findById(long id);

    List<Project> findAll();

    default boolean exists(long id) {
        return findById(id).isPresent();
    }
}
