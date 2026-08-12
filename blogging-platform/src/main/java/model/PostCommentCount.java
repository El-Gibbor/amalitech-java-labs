package model;

/**
 * One row of the "most commented posts" analytics report. This is a query
 * result, not a table row, so unlike Post it is immutable and has no setters.
 */
public class PostCommentCount {
    private final int postId;
    private final String title;
    private final int commentCount;

    public PostCommentCount(int postId, String title, int commentCount) {
        this.postId = postId;
        this.title = title;
        this.commentCount = commentCount;
    }

    public int getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public int getCommentCount() {
        return commentCount;
    }
}
