package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dao.TagDao;
import model.Tag;

/**
 * Runs against a real PostgreSQL database, same approach as PostServiceTest.
 * Every test that writes a row deletes it again before finishing.
 */
class TagServiceTest {
    // Seeded with no tags at all, so assigning and removing here never
    // disturbs a real post's real tag list.
    private static final int UNTAGGED_POST_ID = 6;
    private static final int SEEDED_TAG_ID = 1; // "Databases"

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(new TagDao());
    }

    @Test
    void createTag_thenDeleteTag_roundTrips() {
        Tag created = tagService.createTag("JUnit Test Tag");
        try {
            assertTrue(created.getTagId() > 0, "a saved tag should be given a real id");
            assertEquals("junit-test-tag", created.getSlug(), "the slug should be derived from the name");
        } finally {
            tagService.deleteTag(created.getTagId());
        }

        List<Tag> remaining = tagService.listAllTags();
        assertTrue(remaining.stream().noneMatch(tag -> tag.getName().equals("JUnit Test Tag")),
                "the tag should be gone after deleting it");
    }

    @Test
    void createTag_blankName_isRejected() {
        assertThrows(ValidationException.class, () -> tagService.createTag("   "));
    }

    @Test
    void createTag_duplicateName_isRejected() {
        Tag created = tagService.createTag("Duplicate Name Test");
        try {
            assertThrows(TagServiceException.class, () -> tagService.createTag("Duplicate Name Test"));
        } finally {
            tagService.deleteTag(created.getTagId());
        }
    }

    @Test
    void assignTagToPost_thenRemoveTagFromPost_roundTrips() {
        List<Tag> before = tagService.getTagsForPost(UNTAGGED_POST_ID);
        assertTrue(before.isEmpty(), "this post is seeded with no tags");

        tagService.assignTagToPost(UNTAGGED_POST_ID, SEEDED_TAG_ID);
        try {
            List<Tag> afterAssign = tagService.getTagsForPost(UNTAGGED_POST_ID);
            assertEquals(1, afterAssign.size());
            assertEquals(SEEDED_TAG_ID, afterAssign.get(0).getTagId());
        } finally {
            tagService.removeTagFromPost(UNTAGGED_POST_ID, SEEDED_TAG_ID);
        }

        assertTrue(tagService.getTagsForPost(UNTAGGED_POST_ID).isEmpty(),
                "the tag should no longer be attached after removing it");
    }

    @Test
    void assignTagToPost_sameTagTwice_isRejected() {
        tagService.assignTagToPost(UNTAGGED_POST_ID, SEEDED_TAG_ID);
        try {
            assertThrows(TagServiceException.class,
                    () -> tagService.assignTagToPost(UNTAGGED_POST_ID, SEEDED_TAG_ID));
        } finally {
            tagService.removeTagFromPost(UNTAGGED_POST_ID, SEEDED_TAG_ID);
        }
    }

    @Test
    void removeTagFromPost_notAssigned_isRejected() {
        assertThrows(ValidationException.class,
                () -> tagService.removeTagFromPost(UNTAGGED_POST_ID, SEEDED_TAG_ID));
    }

    @Test
    void deleteTag_nonExistentId_isRejected() {
        assertThrows(ValidationException.class, () -> tagService.deleteTag(999_999));
    }

    @Test
    void listAllTags_isOrderedAlphabetically() {
        List<Tag> tags = tagService.listAllTags();
        for (int i = 1; i < tags.size(); i++) {
            String previous = tags.get(i - 1).getName();
            String current = tags.get(i).getName();
            assertFalse(previous.compareToIgnoreCase(current) > 0,
                    "\"" + previous + "\" should not sort after \"" + current + "\"");
        }
    }
}
