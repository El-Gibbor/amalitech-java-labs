package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.CommentDao;
import model.Comment;

/**
 * Runs against a real PostgreSQL database, same approach as PostServiceTest.
 * Every test that writes a row deletes it again before finishing.
 */
class CommentServiceTest {
    private static final int SEEDED_USER_ID = 2;
    private static final int SEEDED_POST_ID = 1;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(new CommentDao());
    }

    @Test
    void addComment_thenDeleteComment_roundTrips() {
        List<Comment> before = commentService.getCommentsForPost(SEEDED_POST_ID);

        Comment created = commentService.addComment(new Comment(SEEDED_POST_ID, SEEDED_USER_ID, "JUnit test comment."));
        try {
            assertTrue(created.getCommentId() > 0, "a saved comment should be given a real id");
            List<Comment> afterAdd = commentService.getCommentsForPost(SEEDED_POST_ID);
            assertEquals(before.size() + 1, afterAdd.size());
        } finally {
            commentService.deleteComment(created.getCommentId());
        }

        List<Comment> afterDelete = commentService.getCommentsForPost(SEEDED_POST_ID);
        assertEquals(before.size(), afterDelete.size());
    }

    @Test
    void addComment_blankContent_isRejected() {
        Comment comment = new Comment(SEEDED_POST_ID, SEEDED_USER_ID, "   ");
        assertThrows(ValidationException.class, () -> commentService.addComment(comment));
    }

    @Test
    void addComment_contentOverMaxLength_isRejected() {
        Comment comment = new Comment(SEEDED_POST_ID, SEEDED_USER_ID, "x".repeat(2001));
        assertThrows(ValidationException.class, () -> commentService.addComment(comment));
    }

    @Test
    void addComment_unknownPostId_translatesTheForeignKeyViolation() {
        Comment comment = new Comment(999_999, SEEDED_USER_ID, "Attached to no real post.");
        assertThrows(CommentServiceException.class, () -> commentService.addComment(comment));
    }

    @Test
    void updateComment_changesPersistAndCanBeReadBack() {
        Comment created = commentService.addComment(new Comment(SEEDED_POST_ID, SEEDED_USER_ID, "Before edit."));
        try {
            created.setContent("After edit.");
            commentService.updateComment(created);

            Comment reloaded = commentService.getComment(created.getCommentId()).orElseThrow();
            assertEquals("After edit.", reloaded.getContent());
        } finally {
            commentService.deleteComment(created.getCommentId());
        }
    }

    @Test
    void updateComment_nonExistentId_isRejected() {
        Comment phantom = new Comment(SEEDED_POST_ID, SEEDED_USER_ID, "Does not exist.");
        phantom.setCommentId(999_999);
        assertThrows(ValidationException.class, () -> commentService.updateComment(phantom));
    }

    @Test
    void deleteComment_nonExistentId_isRejected() {
        assertThrows(ValidationException.class, () -> commentService.deleteComment(999_999));
    }

    @Test
    void getCommentsForPost_returnsThemOldestFirst() {
        List<Comment> comments = commentService.getCommentsForPost(SEEDED_POST_ID);
        for (int i = 1; i < comments.size(); i++) {
            boolean inOrder = !comments.get(i - 1).getCreatedAt().isAfter(comments.get(i).getCreatedAt());
            assertTrue(inOrder, "comments should read oldest first, like a thread");
        }
    }
}
