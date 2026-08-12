package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.AnalyticsDao;
import model.AuthorPostCount;
import model.PostCommentCount;
import model.PostRatingSummary;
import model.TagUsageCount;

/**
 * Runs against a real PostgreSQL database, same approach as PostServiceTest.
 * These reports are read only, so nothing here writes or cleans up a row.
 * Assertions check structural correctness, ordering and the limit, rather
 * than exact counts, since the database may hold more than just the seed
 * data by the time this runs.
 */
class AnalyticsServiceTest {
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(new AnalyticsDao());
    }

    @Test
    void mostCommentedPosts_respectsTheLimitAndDescendingOrder() {
        List<PostCommentCount> results = analyticsService.mostCommentedPosts(3);
        assertTrue(results.size() <= 3, "should never return more rows than the requested limit");
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getCommentCount() >= results.get(i).getCommentCount(),
                    "each row should have no fewer comments than the row after it");
        }
    }

    @Test
    void highestRatedPosts_onlyIncludesPostsWithAtLeastOneReview() {
        List<PostRatingSummary> results = analyticsService.highestRatedPosts(10);
        for (PostRatingSummary summary : results) {
            assertTrue(summary.getReviewCount() >= 1, "a post with no reviews has nothing to average");
        }
    }

    @Test
    void highestRatedPosts_isOrderedByDescendingAverageRating() {
        List<PostRatingSummary> results = analyticsService.highestRatedPosts(10);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getAverageRating() >= results.get(i).getAverageRating(),
                    "each row should rate no lower than the row after it");
        }
    }

    @Test
    void mostUsedTags_includesASeededTagWhenTheLimitIsHighEnough() {
        List<TagUsageCount> results = analyticsService.mostUsedTags(100);
        assertTrue(results.stream().anyMatch(t -> t.getTagName().equals("Databases")),
                "a tag seeded onto real posts should show up in its own usage report");
    }

    @Test
    void mostActiveAuthors_isOrderedByDescendingPostCount() {
        List<AuthorPostCount> results = analyticsService.mostActiveAuthors(10);
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).getPostCount() >= results.get(i).getPostCount(),
                    "each row should have no fewer posts than the row after it");
        }
    }
}
