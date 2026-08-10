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

import model.Comment;
import util.DatabaseConnection;

/**
 * Data access for the comments table. Every method opens its own connection
 * with try-with-resources and closes it before returning.
 */
public class CommentDao {

    public Comment create(Comment comment) throws SQLException {
        String sql = "INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?) "
                + "RETURNING comment_id, created_at, updated_at";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, comment.getPostId());
            statement.setInt(2, comment.getUserId());
            statement.setString(3, comment.getContent());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                comment.setCommentId(resultSet.getInt("comment_id"));
                comment.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                comment.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
            }
        }
        return comment;
    }

    public Optional<Comment> findById(int commentId) throws SQLException {
        String sql = "SELECT comment_id, post_id, user_id, content, created_at, updated_at "
                + "FROM comments WHERE comment_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, commentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /** Every comment on one post, oldest first, matching how a comment thread reads. */
    public List<Comment> findByPostId(int postId) throws SQLException {
        String sql = "SELECT comment_id, post_id, user_id, content, created_at, updated_at "
                + "FROM comments WHERE post_id = ? ORDER BY created_at ASC";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Comment> comments = new ArrayList<>();
                while (resultSet.next()) {
                    comments.add(mapRow(resultSet));
                }
                return comments;
            }
        }
    }

    public boolean update(Comment comment) throws SQLException {
        String sql = "UPDATE comments SET content = ?, updated_at = ? WHERE comment_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, comment.getContent());
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(3, comment.getCommentId());

            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(int commentId) throws SQLException {
        String sql = "DELETE FROM comments WHERE comment_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, commentId);
            return statement.executeUpdate() == 1;
        }
    }

    private Comment mapRow(ResultSet resultSet) throws SQLException {
        Comment comment = new Comment();
        comment.setCommentId(resultSet.getInt("comment_id"));
        comment.setPostId(resultSet.getInt("post_id"));
        comment.setUserId(resultSet.getInt("user_id"));
        comment.setContent(resultSet.getString("content"));
        comment.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        comment.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return comment;
    }
}
