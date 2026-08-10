package service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import dao.CommentDao;
import model.Comment;

/**
 * Business rules for comments. No SQL here, CommentDao is the only thing in
 * this class that knows a database exists at all.
 */
public class CommentService {
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final CommentDao commentDao;

    public CommentService(CommentDao commentDao) {
        this.commentDao = commentDao;
    }

    public Comment addComment(Comment comment) {
        validate(comment);
        try {
            return commentDao.create(comment);
        } catch (SQLException e) {
            throw translate(e, "add the comment");
        }
    }

    public Optional<Comment> getComment(int commentId) {
        try {
            return commentDao.findById(commentId);
        } catch (SQLException e) {
            throw new CommentServiceException("Could not load the comment.", e);
        }
    }

    public List<Comment> getCommentsForPost(int postId) {
        try {
            return commentDao.findByPostId(postId);
        } catch (SQLException e) {
            throw new CommentServiceException("Could not load comments for this post.", e);
        }
    }

    public Comment updateComment(Comment comment) {
        validate(comment);
        if (comment.getCommentId() == null) {
            throw new ValidationException("Cannot update a comment that has no id yet.");
        }
        try {
            boolean updated = commentDao.update(comment);
            if (!updated) {
                throw new ValidationException("Comment " + comment.getCommentId() + " does not exist.");
            }
            return comment;
        } catch (SQLException e) {
            throw translate(e, "update the comment");
        }
    }

    public void deleteComment(int commentId) {
        try {
            boolean deleted = commentDao.delete(commentId);
            if (!deleted) {
                throw new ValidationException("Comment " + commentId + " does not exist.");
            }
        } catch (SQLException e) {
            throw new CommentServiceException("Could not delete the comment.", e);
        }
    }

    private void validate(Comment comment) {
        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new ValidationException("Comment cannot be blank.");
        }
        if (comment.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new ValidationException("Comment cannot be longer than " + MAX_CONTENT_LENGTH + " characters.");
        }
    }

    private CommentServiceException translate(SQLException e, String action) {
        if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
            return new CommentServiceException(
                    "Could not " + action + ": the referenced post or user does not exist.", e);
        }
        return new CommentServiceException("Could not " + action + ".", e);
    }
}
