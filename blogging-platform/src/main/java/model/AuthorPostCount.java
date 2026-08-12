package model;

/**
 * One row of the "most active authors" analytics report. This is a query
 * result, not a table row, backed by users joined to posts.
 */
public class AuthorPostCount {
    private final String username;
    private final int postCount;

    public AuthorPostCount(String username, int postCount) {
        this.username = username;
        this.postCount = postCount;
    }

    public String getUsername() {
        return username;
    }

    public int getPostCount() {
        return postCount;
    }
}
