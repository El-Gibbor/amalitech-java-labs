package service;

import java.sql.SQLException;
import java.util.List;

import dao.TagDao;
import model.Tag;

/**
 * Business rules for tags and their assignment to posts. No SQL here, TagDao
 * is the only thing in this class that knows a database exists at all.
 */
public class TagService {
    private static final int MAX_NAME_LENGTH = 100;
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";

    private final TagDao tagDao;

    public TagService(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    public Tag createTag(String name) {
        String trimmed = validateName(name);
        Tag tag = new Tag(trimmed, slugify(trimmed));
        try {
            return tagDao.create(tag);
        } catch (SQLException e) {
            throw translate(e, "create the tag", "A tag named \"" + trimmed + "\" already exists.");
        }
    }

    public List<Tag> listAllTags() {
        try {
            return tagDao.findAll();
        } catch (SQLException e) {
            throw new TagServiceException("Could not load tags.", e);
        }
    }

    public List<Tag> getTagsForPost(int postId) {
        try {
            return tagDao.findByPostId(postId);
        } catch (SQLException e) {
            throw new TagServiceException("Could not load tags for this post.", e);
        }
    }

    public void assignTagToPost(int postId, int tagId) {
        try {
            tagDao.assignToPost(postId, tagId);
        } catch (SQLException e) {
            throw translate(e, "assign the tag", "That tag is already assigned to this post.");
        }
    }

    public void removeTagFromPost(int postId, int tagId) {
        try {
            boolean removed = tagDao.removeFromPost(postId, tagId);
            if (!removed) {
                throw new ValidationException("That tag is not assigned to this post.");
            }
        } catch (SQLException e) {
            throw new TagServiceException("Could not remove the tag from the post.", e);
        }
    }

    public void deleteTag(int tagId) {
        try {
            boolean deleted = tagDao.delete(tagId);
            if (!deleted) {
                throw new ValidationException("Tag " + tagId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new TagServiceException("Could not delete the tag.", e);
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Tag name cannot be blank.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Tag name cannot be longer than " + MAX_NAME_LENGTH + " characters.");
        }
        return trimmed;
    }

    // Lowercases, replaces every run of non alphanumeric characters with a single
    // hyphen, and trims leading/trailing hyphens, e.g. "Say Hello!!" -> "say-hello".
    private String slugify(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "tag" : slug;
    }

    private TagServiceException translate(SQLException e, String action, String uniqueViolationMessage) {
        if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
            return new TagServiceException(uniqueViolationMessage, e);
        }
        if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
            return new TagServiceException("Could not " + action + ": the referenced post or tag does not exist.", e);
        }
        return new TagServiceException("Could not " + action + ".", e);
    }
}
