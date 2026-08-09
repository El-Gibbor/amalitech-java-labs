package service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import dao.PostDao;
import model.Post;

/**
 * Business rules for posts. No SQL here, PostDao is the only thing in this
 * class that knows a database exists at all.
 */
public class PostService {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final PostDao postDao;

    public PostService(PostDao postDao) {
        this.postDao = postDao;
    }

    public Post createPost(Post post) {
        validate(post);
        try {
            return postDao.create(post);
        } catch (SQLException e) {
            throw translate(e, "create the post");
        }
    }

    public Optional<Post> getPost(int postId) {
        try {
            return postDao.findById(postId);
        } catch (SQLException e) {
            throw new PostServiceException("Could not load the post.", e);
        }
    }

    /**
     * @param pageNumber one based, the first page is 1, not 0
     * @param pageSize   how many posts belong on one page
     */
    public List<Post> listPosts(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            throw new ValidationException("Page number must be 1 or greater.");
        }
        int offset = (pageNumber - 1) * pageSize;
        try {
            return postDao.findAll(pageSize, offset);
        } catch (SQLException e) {
            throw new PostServiceException("Could not load posts.", e);
        }
    }

    public Post updatePost(Post post) {
        validate(post);
        if (post.getPostId() == null) {
            throw new ValidationException("Cannot update a post that has no id yet.");
        }
        try {
            boolean updated = postDao.update(post);
            if (!updated) {
                throw new ValidationException("Post " + post.getPostId() + " does not exist.");
            }
            return post;
        } catch (SQLException e) {
            throw translate(e, "update the post");
        }
    }

    public void deletePost(int postId) {
        try {
            boolean deleted = postDao.delete(postId);
            if (!deleted) {
                throw new ValidationException("Post " + postId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new PostServiceException("Could not delete the post.", e);
        }
    }

    private void validate(Post post) {
        if (post.getTitle() == null || post.getTitle().isBlank()) {
            throw new ValidationException("Title cannot be blank.");
        }
        if (post.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Title cannot be longer than " + MAX_TITLE_LENGTH + " characters.");
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new ValidationException("Content cannot be blank.");
        }
    }

    private PostServiceException translate(SQLException e, String action) {
        if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
            return new PostServiceException("Could not " + action + ": the referenced user does not exist.", e);
        }
        return new PostServiceException("Could not " + action + ".", e);
    }
}
