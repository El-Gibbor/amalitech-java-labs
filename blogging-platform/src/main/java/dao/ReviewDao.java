package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import model.Review;
import util.DatabaseConnection;

/**
 * Data access for the reviews table. Every method opens its own connection
 * with try-with-resources and closes it before returning.
 */
public class ReviewDao {

    public Review create(Review review) throws SQLException {
        String sql = "INSERT INTO reviews (post_id, user_id, rating, review_text) VALUES (?, ?, ?, ?) "
                + "RETURNING review_id, created_at, updated_at";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, review.getPostId());
            statement.setInt(2, review.getUserId());
            statement.setInt(3, review.getRating());
            statement.setString(4, review.getReviewText());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                review.setReviewId(resultSet.getInt("review_id"));
                review.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                review.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
            }
        }
        return review;
    }

    public Optional<Review> findById(int reviewId) throws SQLException {
        String sql = "SELECT review_id, post_id, user_id, rating, review_text, created_at, updated_at "
                + "FROM reviews WHERE review_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, reviewId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /** Every review left on one post, oldest first. */
    public List<Review> findByPostId(int postId) throws SQLException {
        String sql = "SELECT review_id, post_id, user_id, rating, review_text, created_at, updated_at "
                + "FROM reviews WHERE post_id = ? ORDER BY created_at ASC";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (resultSet.next()) {
                    reviews.add(mapRow(resultSet));
                }
                return reviews;
            }
        }
    }

    /** The mean rating left on one post, empty if it has no reviews yet. */
    public OptionalDouble averageRatingForPost(int postId) throws SQLException {
        String sql = "SELECT AVG(rating) AS average_rating FROM reviews WHERE post_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                double average = resultSet.getDouble("average_rating");
                return resultSet.wasNull() ? OptionalDouble.empty() : OptionalDouble.of(average);
            }
        }
    }

    public boolean update(Review review) throws SQLException {
        String sql = "UPDATE reviews SET rating = ?, review_text = ?, updated_at = ? WHERE review_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, review.getRating());
            statement.setString(2, review.getReviewText());
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(4, review.getReviewId());

            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(int reviewId) throws SQLException {
        String sql = "DELETE FROM reviews WHERE review_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, reviewId);
            return statement.executeUpdate() == 1;
        }
    }

    private Review mapRow(ResultSet resultSet) throws SQLException {
        Review review = new Review();
        review.setReviewId(resultSet.getInt("review_id"));
        review.setPostId(resultSet.getInt("post_id"));
        review.setUserId(resultSet.getInt("user_id"));
        review.setRating(resultSet.getInt("rating"));
        review.setReviewText(resultSet.getString("review_text"));
        review.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        review.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return review;
    }
}
