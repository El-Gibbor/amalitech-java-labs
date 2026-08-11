package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.PostDao;
import model.Post;

/**
 * Runs against a real PostgreSQL database, the same one the application
 * itself uses, configured through db.properties. There are no mocks here,
 * this project verifies actual SQL behavior, actual constraints, and actual
 * query results, not a simulation of them. Every test that writes a row
 * deletes it again before finishing, so the suite can be run repeatedly
 * without drifting the seed data other tests and the application rely on.
 */
class PostServiceTest {
    private static final int SEEDED_USER_ID = 1;

    private PostService postService;

    @BeforeEach
    void setUp() {
        // A fresh instance per test, since PostService caches internally and a
        // shared instance would let one test's cache state leak into another.
        postService = new PostService(new PostDao());
    }

    @Test
    void createPost_thenDeletePost_roundTrips() {
        Post post = new Post(SEEDED_USER_ID, "JUnit test post", "Created and removed by a test.", null);

        Post created = postService.createPost(post);
        try {
            assertTrue(created.getPostId() > 0, "a saved post should be given a real id");
            assertEquals("JUnit test post", created.getTitle());
            assertNull(created.getPublishedAt(), "no published_at was supplied, so it should stay null");
        } finally {
            postService.deletePost(created.getPostId());
        }

        assertTrue(postService.getPost(created.getPostId()).isEmpty(), "the post should be gone after deleting it");
    }

    @Test
    void createPost_blankTitle_isRejectedBeforeReachingTheDatabase() {
        Post post = new Post(SEEDED_USER_ID, "   ", "Some content.", null);
        assertThrows(ValidationException.class, () -> postService.createPost(post));
    }

    @Test
    void createPost_blankContent_isRejectedBeforeReachingTheDatabase() {
        Post post = new Post(SEEDED_USER_ID, "A title", "   ", null);
        assertThrows(ValidationException.class, () -> postService.createPost(post));
    }

    @Test
    void createPost_titleOverMaxLength_isRejected() {
        String tooLong = "x".repeat(256);
        Post post = new Post(SEEDED_USER_ID, tooLong, "Some content.", null);
        assertThrows(ValidationException.class, () -> postService.createPost(post));
    }

    @Test
    void createPost_unknownUserId_translatesTheForeignKeyViolation() {
        Post post = new Post(999_999, "Orphan post", "No such user exists.", null);
        PostServiceException exception = assertThrows(PostServiceException.class, () -> postService.createPost(post));
        assertTrue(exception.getMessage().contains("does not exist"),
                "the message should explain the real cause, not just say the save failed");
    }

    @Test
    void updatePost_changesPersistAndCanBeReadBack() {
        Post created = postService.createPost(new Post(SEEDED_USER_ID, "Before update", "Original content.", null));
        try {
            created.setTitle("After update");
            created.setContent("Changed content.");
            postService.updatePost(created);

            Post reloaded = postService.getPost(created.getPostId()).orElseThrow();
            assertEquals("After update", reloaded.getTitle());
            assertEquals("Changed content.", reloaded.getContent());
        } finally {
            postService.deletePost(created.getPostId());
        }
    }

    @Test
    void updatePost_publishingADraft_setsPublishedAt() {
        Post created = postService.createPost(new Post(SEEDED_USER_ID, "Draft post", "Not published yet.", null));
        try {
            assertNull(created.getPublishedAt());
            created.setPublishedAt(LocalDateTime.now());
            Post updated = postService.updatePost(created);
            assertTrue(postService.getPost(updated.getPostId()).orElseThrow().getPublishedAt() != null);
        } finally {
            postService.deletePost(created.getPostId());
        }
    }

    @Test
    void updatePost_nonExistentId_isRejected() {
        Post phantom = new Post(SEEDED_USER_ID, "Does not exist", "Content.", null);
        phantom.setPostId(999_999);
        assertThrows(ValidationException.class, () -> postService.updatePost(phantom));
    }

    @Test
    void deletePost_nonExistentId_isRejected() {
        assertThrows(ValidationException.class, () -> postService.deletePost(999_999));
    }

    @Test
    void listPosts_respectsThePageSize() {
        List<Post> page = postService.listPosts(1, 3);
        assertTrue(page.size() <= 3, "a page of size 3 should never return more than 3 rows");
    }

    @Test
    void listPosts_rejectsAPageNumberBelowOne() {
        assertThrows(ValidationException.class, () -> postService.listPosts(0, 5));
    }

    @Test
    void searchByKeyword_requiresANonBlankTerm() {
        assertThrows(ValidationException.class, () -> postService.searchByKeyword("  ", 1, 5));
    }

    @Test
    void searchByKeyword_isCaseInsensitive() {
        Post created = postService.createPost(
                new Post(SEEDED_USER_ID, "Findable By Keyword", "Unique marker content for this test.", null));
        try {
            List<Post> results = postService.searchByKeyword("findable by keyword", 1, 10);
            assertTrue(results.stream().anyMatch(p -> p.getPostId().equals(created.getPostId())),
                    "a lowercase search term should still match a mixed case title");
        } finally {
            postService.deletePost(created.getPostId());
        }
    }

    @Test
    void listPosts_secondIdenticalCallIsServedFromCache() {
        int hitsBefore = postService.getCacheHits();
        int missesBefore = postService.getCacheMisses();

        postService.listPosts(1, 5);
        postService.listPosts(1, 5);

        assertEquals(missesBefore + 1, postService.getCacheMisses(), "the first call should be a miss");
        assertEquals(hitsBefore + 1, postService.getCacheHits(), "the second identical call should be a hit");
    }

    @Test
    void creatingAPost_invalidatesThePageCache() {
        postService.listPosts(1, 5);
        int missesBefore = postService.getCacheMisses();

        Post created = postService.createPost(new Post(SEEDED_USER_ID, "Cache buster", "Forces a cache clear.", null));
        try {
            postService.listPosts(1, 5);
            assertEquals(missesBefore + 1, postService.getCacheMisses(),
                    "a write should clear the cache, so this repeat call must miss again, not hit");
        } finally {
            postService.deletePost(created.getPostId());
        }
    }

    @Test
    void sortingByTitle_ordersTheFullResultSetAlphabetically() {
        List<Post> sorted = postService.listPosts(1, 100, PostService.SortOption.TITLE_A_TO_Z);
        for (int i = 1; i < sorted.size(); i++) {
            String previous = sorted.get(i - 1).getTitle();
            String current = sorted.get(i).getTitle();
            assertFalse(previous.compareToIgnoreCase(current) > 0,
                    "\"" + previous + "\" should not sort after \"" + current + "\"");
        }
    }

    @Test
    void sortingByPublishedDate_putsDraftsLast() {
        List<Post> sorted = postService.listPosts(1, 100, PostService.SortOption.PUBLISHED_EARLIEST_FIRST);
        boolean sawDraft = false;
        for (Post post : sorted) {
            if (post.getPublishedAt() == null) {
                sawDraft = true;
            } else if (sawDraft) {
                throw new AssertionError("a published post appeared after a draft in earliest first order");
            }
        }
    }
}
