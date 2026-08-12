package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import model.Tag;
import util.DatabaseConnection;

/**
 * Data access for the tags table and the post_tags join table. Every method
 * opens its own connection with try-with-resources and closes it before
 * returning.
 */
public class TagDao {

    public Tag create(Tag tag) throws SQLException {
        String sql = "INSERT INTO tags (name, slug) VALUES (?, ?) RETURNING tag_id, created_at";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tag.getName());
            statement.setString(2, tag.getSlug());

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                tag.setTagId(resultSet.getInt("tag_id"));
                tag.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
            }
        }
        return tag;
    }

    public Optional<Tag> findById(int tagId) throws SQLException {
        String sql = "SELECT tag_id, name, slug, created_at FROM tags WHERE tag_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, tagId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /** Every tag that exists, alphabetically, regardless of whether any post uses it. */
    public List<Tag> findAll() throws SQLException {
        String sql = "SELECT tag_id, name, slug, created_at FROM tags ORDER BY name ASC";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            List<Tag> tags = new ArrayList<>();
            while (resultSet.next()) {
                tags.add(mapRow(resultSet));
            }
            return tags;
        }
    }

    /** Every tag assigned to one post, alphabetically. */
    public List<Tag> findByPostId(int postId) throws SQLException {
        String sql = "SELECT t.tag_id, t.name, t.slug, t.created_at "
                + "FROM tags t JOIN post_tags pt ON pt.tag_id = t.tag_id "
                + "WHERE pt.post_id = ? ORDER BY t.name ASC";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<Tag> tags = new ArrayList<>();
                while (resultSet.next()) {
                    tags.add(mapRow(resultSet));
                }
                return tags;
            }
        }
    }

    public boolean delete(int tagId) throws SQLException {
        String sql = "DELETE FROM tags WHERE tag_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, tagId);
            return statement.executeUpdate() == 1;
        }
    }

    /** Links an existing tag to an existing post. Throws if that pair is already linked. */
    public void assignToPost(int postId, int tagId) throws SQLException {
        String sql = "INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            statement.setInt(2, tagId);
            statement.executeUpdate();
        }
    }

    public boolean removeFromPost(int postId, int tagId) throws SQLException {
        String sql = "DELETE FROM post_tags WHERE post_id = ? AND tag_id = ?";

        try (Connection connection = DatabaseConnection.get();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            statement.setInt(2, tagId);
            return statement.executeUpdate() == 1;
        }
    }

    private Tag mapRow(ResultSet resultSet) throws SQLException {
        Tag tag = new Tag();
        tag.setTagId(resultSet.getInt("tag_id"));
        tag.setName(resultSet.getString("name"));
        tag.setSlug(resultSet.getString("slug"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        tag.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return tag;
    }
}
