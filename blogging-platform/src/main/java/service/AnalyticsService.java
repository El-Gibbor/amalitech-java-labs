package service;

import java.sql.SQLException;
import java.util.List;

import dao.AnalyticsDao;
import model.AuthorPostCount;
import model.PostCommentCount;
import model.PostRatingSummary;
import model.TagUsageCount;

/**
 * Read only aggregate reporting over posts, comments, tags, reviews, and
 * users. No SQL here, AnalyticsDao is the only thing in this class that
 * knows a database exists at all.
 */
public class AnalyticsService {
    private final AnalyticsDao analyticsDao;

    public AnalyticsService(AnalyticsDao analyticsDao) {
        this.analyticsDao = analyticsDao;
    }

    public List<PostCommentCount> mostCommentedPosts(int limit) {
        try {
            return analyticsDao.mostCommentedPosts(limit);
        } catch (SQLException e) {
            throw new AnalyticsServiceException("Could not load the most commented posts report.", e);
        }
    }

    public List<PostRatingSummary> highestRatedPosts(int limit) {
        try {
            return analyticsDao.highestRatedPosts(limit);
        } catch (SQLException e) {
            throw new AnalyticsServiceException("Could not load the highest rated posts report.", e);
        }
    }

    public List<TagUsageCount> mostUsedTags(int limit) {
        try {
            return analyticsDao.mostUsedTags(limit);
        } catch (SQLException e) {
            throw new AnalyticsServiceException("Could not load the most used tags report.", e);
        }
    }

    public List<AuthorPostCount> mostActiveAuthors(int limit) {
        try {
            return analyticsDao.mostActiveAuthors(limit);
        } catch (SQLException e) {
            throw new AnalyticsServiceException("Could not load the most active authors report.", e);
        }
    }
}
