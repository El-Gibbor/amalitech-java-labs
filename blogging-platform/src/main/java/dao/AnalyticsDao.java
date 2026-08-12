package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.AuthorPostCount;
import model.PostCommentCount;
import model.PostRatingSummary;
import model.TagUsageCount;
import util.DatabaseConnection;

/**
 * Read only aggregate queries spanning posts, comments, tags, reviews, and
 * users, backing the Analytics screen. Every method opens its own connection
 * with try-with-resources and closes it before returning.
 */
public class AnalyticsDao {

    /** Posts with the most comments, most commented first. */
    public List<PostCommentCount> mostCommentedPosts(int limit) throws SQLException {
        String sql = "SELECT p.post_id, p.title, COUNT(c.comment_id) AS comment_count "
                + "FROM posts p LEFT JOIN comments c ON c.post_id = p.post_id "
                + "GROUP BY p.post_id, p.title "
                + "ORDER BY comment_count DESC, p.post_id ASC "
                + "LIMIT ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<PostCommentCount> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new PostCommentCount(
                            resultSet.getInt("post_id"),
                            resultSet.getString("title"),
                            resultSet.getInt("comment_count")));
                }
                return results;
            }
        }
    }

    /** Posts with at least one review, highest average rating first. */
    public List<PostRatingSummary> highestRatedPosts(int limit) throws SQLException {
        String sql = "SELECT p.post_id, p.title, AVG(r.rating) AS average_rating, COUNT(r.review_id) AS review_count "
                + "FROM posts p JOIN reviews r ON r.post_id = p.post_id "
                + "GROUP BY p.post_id, p.title "
                + "ORDER BY average_rating DESC, review_count DESC "
                + "LIMIT ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<PostRatingSummary> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new PostRatingSummary(
                            resultSet.getInt("post_id"),
                            resultSet.getString("title"),
                            resultSet.getDouble("average_rating"),
                            resultSet.getInt("review_count")));
                }
                return results;
            }
        }
    }

    /** Tags ranked by how many posts use them, most used first. */
    public List<TagUsageCount> mostUsedTags(int limit) throws SQLException {
        String sql = "SELECT t.name AS tag_name, COUNT(pt.post_id) AS post_count "
                + "FROM tags t LEFT JOIN post_tags pt ON pt.tag_id = t.tag_id "
                + "GROUP BY t.tag_id, t.name "
                + "ORDER BY post_count DESC, t.name ASC "
                + "LIMIT ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<TagUsageCount> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new TagUsageCount(
                            resultSet.getString("tag_name"),
                            resultSet.getInt("post_count")));
                }
                return results;
            }
        }
    }

    /** Authors ranked by how many posts they have written, most prolific first. */
    public List<AuthorPostCount> mostActiveAuthors(int limit) throws SQLException {
        String sql = "SELECT u.username, COUNT(p.post_id) AS post_count "
                + "FROM users u LEFT JOIN posts p ON p.user_id = u.user_id "
                + "GROUP BY u.user_id, u.username "
                + "ORDER BY post_count DESC, u.username ASC "
                + "LIMIT ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<AuthorPostCount> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new AuthorPostCount(
                            resultSet.getString("username"),
                            resultSet.getInt("post_count")));
                }
                return results;
            }
        }
    }
}
