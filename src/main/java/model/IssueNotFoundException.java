package model;

public class IssueNotFoundException extends RuntimeException {
    public IssueNotFoundException(long issueId) {
        super("Issue not found: " + issueId);
    }
}
