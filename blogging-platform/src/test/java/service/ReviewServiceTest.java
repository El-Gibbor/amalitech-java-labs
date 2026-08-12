package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.ReviewDao;
import model.Review;

/**
 * Runs against a real PostgreSQL database, same approach as CommentServiceTest.
 * Every test that writes a row deletes it again before finishing.
 */
class ReviewServiceTest {
    private static final int SEEDED_USER_ID = 4;
    // Seeded with no reviews at all, so adding and removing here never
    // disturbs a real post's real review list.
    private static final int UNREVIEWED_POST_ID = 6;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(new ReviewDao());
    }

    @Test
    void addReview_thenDeleteReview_roundTrips() {
        List<Review> before = reviewService.getReviewsForPost(UNREVIEWED_POST_ID);
        assertTrue(before.isEmpty(), "this post is seeded with no reviews");

        Review created = reviewService.addReview(new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 4, "Solid read."));
        try {
            assertTrue(created.getReviewId() > 0, "a saved review should be given a real id");
            List<Review> afterAdd = reviewService.getReviewsForPost(UNREVIEWED_POST_ID);
            assertEquals(1, afterAdd.size());
        } finally {
            reviewService.deleteReview(created.getReviewId());
        }

        assertTrue(reviewService.getReviewsForPost(UNREVIEWED_POST_ID).isEmpty(),
                "the review should be gone after deleting it");
    }

    @Test
    void addReview_ratingOutOfRange_isRejected() {
        Review tooHigh = new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 6, "Impossible rating.");
        assertThrows(ValidationException.class, () -> reviewService.addReview(tooHigh));

        Review tooLow = new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 0, "Impossible rating.");
        assertThrows(ValidationException.class, () -> reviewService.addReview(tooLow));
    }

    @Test
    void addReview_sameUserTwiceOnSamePost_isRejected() {
        Review created = reviewService.addReview(new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 3, "First review."));
        try {
            Review second = new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 5, "Second review, same user.");
            assertThrows(ReviewServiceException.class, () -> reviewService.addReview(second));
        } finally {
            reviewService.deleteReview(created.getReviewId());
        }
    }

    @Test
    void addReview_unknownPostId_translatesTheForeignKeyViolation() {
        Review review = new Review(999_999, SEEDED_USER_ID, 5, "Attached to no real post.");
        assertThrows(ReviewServiceException.class, () -> reviewService.addReview(review));
    }

    @Test
    void updateReview_changesPersistAndCanBeReadBack() {
        Review created = reviewService.addReview(new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 2, "Before edit."));
        try {
            created.setRating(5);
            created.setReviewText("After edit.");
            reviewService.updateReview(created);

            Review reloaded = reviewService.getReview(created.getReviewId()).orElseThrow();
            assertEquals(5, reloaded.getRating());
            assertEquals("After edit.", reloaded.getReviewText());
        } finally {
            reviewService.deleteReview(created.getReviewId());
        }
    }

    @Test
    void updateReview_nonExistentId_isRejected() {
        Review phantom = new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 3, "Does not exist.");
        phantom.setReviewId(999_999);
        assertThrows(ValidationException.class, () -> reviewService.updateReview(phantom));
    }

    @Test
    void deleteReview_nonExistentId_isRejected() {
        assertThrows(ValidationException.class, () -> reviewService.deleteReview(999_999));
    }

    @Test
    void getAverageRating_reflectsAllReviewsOnThatPost() {
        Review first = reviewService.addReview(new Review(UNREVIEWED_POST_ID, SEEDED_USER_ID, 2, null));
        try {
            OptionalDouble average = reviewService.getAverageRating(UNREVIEWED_POST_ID);
            assertTrue(average.isPresent());
            assertEquals(2.0, average.getAsDouble());
        } finally {
            reviewService.deleteReview(first.getReviewId());
        }

        assertTrue(reviewService.getAverageRating(UNREVIEWED_POST_ID).isEmpty(),
                "with no reviews left, there is nothing to average");
    }
}
