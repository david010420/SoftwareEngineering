package model;

import java.io.Serializable;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    private final String name;

    public Project(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("project name is required");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public String getId() { return this.id;}

    @Override
    public String toString() {
        return name;
    }
}
