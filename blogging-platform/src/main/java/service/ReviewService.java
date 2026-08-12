package service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import dao.ReviewDao;
import model.Review;

/**
 * Business rules for reviews. No SQL here, ReviewDao is the only thing in
 * this class that knows a database exists at all.
 */
public class ReviewService {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_REVIEW_TEXT_LENGTH = 2000;
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";

    private final ReviewDao reviewDao;

    public ReviewService(ReviewDao reviewDao) {
        this.reviewDao = reviewDao;
    }

    public Review addReview(Review review) {
        validate(review);
        try {
            return reviewDao.create(review);
        } catch (SQLException e) {
            throw translate(e, "add the review");
        }
    }

    public Optional<Review> getReview(int reviewId) {
        try {
            return reviewDao.findById(reviewId);
        } catch (SQLException e) {
            throw new ReviewServiceException("Could not load the review.", e);
        }
    }

    public List<Review> getReviewsForPost(int postId) {
        try {
            return reviewDao.findByPostId(postId);
        } catch (SQLException e) {
            throw new ReviewServiceException("Could not load reviews for this post.", e);
        }
    }

    /** The mean rating left on one post, empty if it has no reviews yet. */
    public OptionalDouble getAverageRating(int postId) {
        try {
            return reviewDao.averageRatingForPost(postId);
        } catch (SQLException e) {
            throw new ReviewServiceException("Could not compute the average rating for this post.", e);
        }
    }

    public Review updateReview(Review review) {
        validate(review);
        if (review.getReviewId() == null) {
            throw new ValidationException("Cannot update a review that has no id yet.");
        }
        try {
            boolean updated = reviewDao.update(review);
            if (!updated) {
                throw new ValidationException("Review " + review.getReviewId() + " does not exist.");
            }
            return review;
        } catch (SQLException e) {
            throw translate(e, "update the review");
        }
    }

    public void deleteReview(int reviewId) {
        try {
            boolean deleted = reviewDao.delete(reviewId);
            if (!deleted) {
                throw new ValidationException("Review " + reviewId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new ReviewServiceException("Could not delete the review.", e);
        }
    }

    private void validate(Review review) {
        if (review.getRating() < MIN_RATING || review.getRating() > MAX_RATING) {
            throw new ValidationException("Rating must be between " + MIN_RATING + " and " + MAX_RATING + ".");
        }
        if (review.getReviewText() != null && review.getReviewText().length() > MAX_REVIEW_TEXT_LENGTH) {
            throw new ValidationException(
                    "Review text cannot be longer than " + MAX_REVIEW_TEXT_LENGTH + " characters.");
        }
    }

    private ReviewServiceException translate(SQLException e, String action) {
        if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
            return new ReviewServiceException("You have already reviewed this post.", e);
        }
        if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
            return new ReviewServiceException(
                    "Could not " + action + ": the referenced post or user does not exist.", e);
        }
        return new ReviewServiceException("Could not " + action + ".", e);
    }
}
