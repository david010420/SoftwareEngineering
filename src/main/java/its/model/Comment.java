package its.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String author;
    private final String message;
    private final LocalDateTime createdAt;

    public Comment(String author, String message, LocalDateTime createdAt) {
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("author is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        this.author = author.trim();
        this.message = message.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "[" + createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "] "
                + author + ": " + message;
    }
}
