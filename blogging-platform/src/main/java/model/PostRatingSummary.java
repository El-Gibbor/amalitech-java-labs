package model;

/**
 * One row of the "highest rated posts" analytics report. This is a query
 * result, not a table row, so unlike Post it is immutable and has no setters.
 */
public class PostRatingSummary {
    private final int postId;
    private final String title;
    private final double averageRating;
    private final int reviewCount;

    public PostRatingSummary(int postId, String title, double averageRating, int reviewCount) {
        this.postId = postId;
        this.title = title;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public int getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }
}
