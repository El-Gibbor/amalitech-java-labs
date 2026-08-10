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

import model.Post;
import util.DatabaseConnection;

/**
 * Data access for the posts table. Every method opens its own connection
 * with try-with-resources and closes it before returning.
 */
public class PostDao {

    public Post create(Post post) throws SQLException {
        String sql = "INSERT INTO posts (user_id, title, content, published_at) "
                + "VALUES (?, ?, ?, ?) RETURNING post_id, created_at, updated_at";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, post.getUserId());
            statement.setString(2, post.getTitle());
            statement.setString(3, post.getContent());
            setNullableTimestamp(statement, 4, post.getPublishedAt());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                post.setPostId(resultSet.getInt("post_id"));
                post.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
                post.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
            }
        }
        return post;
    }

    public Optional<Post> findById(int postId) throws SQLException {
        String sql = "SELECT post_id, user_id, title, content, published_at, created_at, updated_at "
                + "FROM posts WHERE post_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * @param limit  how many posts to return
     * @param offset how many matching posts to skip before starting to return rows
     * @return posts ordered newest first, one page at a time
     */
    public List<Post> findAll(int limit, int offset) throws SQLException {
        String sql = "SELECT post_id, user_id, title, content, published_at, created_at, updated_at "
                + "FROM posts ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, limit);
            statement.setInt(2, offset);

            return mapRows(statement);
        }
    }

    /** Case-insensitive match against title or content, using PostgreSQL's ILIKE. */
    public List<Post> searchByKeyword(String keyword, int limit, int offset) throws SQLException {
        String sql = "SELECT post_id, user_id, title, content, published_at, created_at, updated_at "
                + "FROM posts WHERE title ILIKE ? OR content ILIKE ? "
                + "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setInt(3, limit);
            statement.setInt(4, offset);

            return mapRows(statement);
        }
    }

    /** Case-insensitive match against the author's username, joining to users. */
    public List<Post> searchByAuthor(String username, int limit, int offset) throws SQLException {
        String sql = "SELECT p.post_id, p.user_id, p.title, p.content, p.published_at, p.created_at, p.updated_at "
                + "FROM posts p JOIN users u ON u.user_id = p.user_id "
                + "WHERE u.username ILIKE ? "
                + "ORDER BY p.created_at DESC LIMIT ? OFFSET ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + username + "%");
            statement.setInt(2, limit);
            statement.setInt(3, offset);

            return mapRows(statement);
        }
    }

    /** Case-insensitive match against a tag name, joining through post_tags to tags. */
    public List<Post> searchByTag(String tagName, int limit, int offset) throws SQLException {
        String sql = "SELECT p.post_id, p.user_id, p.title, p.content, p.published_at, p.created_at, p.updated_at "
                + "FROM posts p "
                + "JOIN post_tags pt ON pt.post_id = p.post_id "
                + "JOIN tags t ON t.tag_id = pt.tag_id "
                + "WHERE t.name ILIKE ? "
                + "ORDER BY p.created_at DESC LIMIT ? OFFSET ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + tagName + "%");
            statement.setInt(2, limit);
            statement.setInt(3, offset);

            return mapRows(statement);
        }
    }

    private List<Post> mapRows(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<Post> posts = new ArrayList<>();
            while (resultSet.next()) {
                posts.add(mapRow(resultSet));
            }
            return posts;
        }
    }

    public boolean update(Post post) throws SQLException {
        String sql = "UPDATE posts SET title = ?, content = ?, published_at = ?, updated_at = ? "
                + "WHERE post_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, post.getTitle());
            statement.setString(2, post.getContent());
            setNullableTimestamp(statement, 3, post.getPublishedAt());
            statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            statement.setInt(5, post.getPostId());

            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(int postId) throws SQLException {
        String sql = "DELETE FROM posts WHERE post_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            return statement.executeUpdate() == 1;
        }
    }

    private Post mapRow(ResultSet resultSet) throws SQLException {
        Post post = new Post();
        post.setPostId(resultSet.getInt("post_id"));
        post.setUserId(resultSet.getInt("user_id"));
        post.setTitle(resultSet.getString("title"));
        post.setContent(resultSet.getString("content"));

        Timestamp publishedAt = resultSet.getTimestamp("published_at");
        post.setPublishedAt(publishedAt == null ? null : publishedAt.toLocalDateTime());

        post.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        post.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return post;
    }

    private void setNullableTimestamp(PreparedStatement statement, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
