package model;

import java.time.LocalDateTime;

/** A plain data holder mirroring one row of the reviews table. No SQL, no validation. */
public class Review {
    private Integer reviewId;
    private int postId;
    private int userId;
    private int rating;
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Review() {
    }

    public Review(int postId, int userId, int rating, String reviewText) {
        this.postId = postId;
        this.userId = userId;
        this.rating = rating;
        this.reviewText = reviewText;
    }

    public Integer getReviewId() {
        return reviewId;
    }

    public void setReviewId(Integer reviewId) {
        this.reviewId = reviewId;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
