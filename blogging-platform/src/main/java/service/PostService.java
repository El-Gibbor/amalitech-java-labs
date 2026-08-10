package service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

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

    // Caches one page of results per query type, term, page number, and page size.
    private final Map<CacheKey, List<Post>> pageCache = new HashMap<>();
    private int cacheHits = 0;
    private int cacheMisses = 0;

    public PostService(PostDao postDao) {
        this.postDao = postDao;
    }

    public Post createPost(Post post) {
        validate(post);
        try {
            Post created = postDao.create(post);
            clearCache();
            return created;
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
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        CacheKey key = new CacheKey(QueryType.BROWSE, null, pageNumber, pageSize);
        return getOrLoad(key, () -> {
            try {
                return postDao.findAll(pageSize, offset);
            } catch (SQLException e) {
                throw new PostServiceException("Could not load posts.", e);
            }
        });
    }

    public List<Post> searchByKeyword(String keyword, int pageNumber, int pageSize) {
        requireSearchTerm(keyword, "keyword");
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        CacheKey key = new CacheKey(QueryType.KEYWORD, keyword, pageNumber, pageSize);
        return getOrLoad(key, () -> {
            try {
                return postDao.searchByKeyword(keyword, pageSize, offset);
            } catch (SQLException e) {
                throw new PostServiceException("Could not search posts by keyword.", e);
            }
        });
    }

    public List<Post> searchByAuthor(String username, int pageNumber, int pageSize) {
        requireSearchTerm(username, "author");
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        CacheKey key = new CacheKey(QueryType.AUTHOR, username, pageNumber, pageSize);
        return getOrLoad(key, () -> {
            try {
                return postDao.searchByAuthor(username, pageSize, offset);
            } catch (SQLException e) {
                throw new PostServiceException("Could not search posts by author.", e);
            }
        });
    }

    public List<Post> searchByTag(String tagName, int pageNumber, int pageSize) {
        requireSearchTerm(tagName, "tag");
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        CacheKey key = new CacheKey(QueryType.TAG, tagName, pageNumber, pageSize);
        return getOrLoad(key, () -> {
            try {
                return postDao.searchByTag(tagName, pageSize, offset);
            } catch (SQLException e) {
                throw new PostServiceException("Could not search posts by tag.", e);
            }
        });
    }

    // Returns the cached page if present, otherwise loads and caches it.
    private List<Post> getOrLoad(CacheKey key, Supplier<List<Post>> loader) {
        if (pageCache.containsKey(key)) {
            cacheHits++;
            return pageCache.get(key);
        }
        cacheMisses++;
        List<Post> result = loader.get();
        pageCache.put(key, result);
        return result;
    }

    // A write can shift what belongs on any page, so the whole cache is dropped.
    private void clearCache() {
        pageCache.clear();
    }

    public int getCacheHits() {
        return cacheHits;
    }

    public int getCacheMisses() {
        return cacheMisses;
    }

    private void requireSearchTerm(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Enter a " + label + " to search for.");
        }
    }

    private int validateAndComputeOffset(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            throw new ValidationException("Page number must be 1 or greater.");
        }
        return (pageNumber - 1) * pageSize;
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
            clearCache();
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
            clearCache();
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

    private enum QueryType {
        BROWSE, KEYWORD, AUTHOR, TAG
    }

    // Uniquely identifies one cached page across every read method.
    private static final class CacheKey {
        private final QueryType type;
        private final String term;
        private final int pageNumber;
        private final int pageSize;

        CacheKey(QueryType type, String term, int pageNumber, int pageSize) {
            this.type = type;
            this.term = term;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheKey)) {
                return false;
            }
            CacheKey that = (CacheKey) other;
            return pageNumber == that.pageNumber
                    && pageSize == that.pageSize
                    && type == that.type
                    && Objects.equals(term, that.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, term, pageNumber, pageSize);
        }
    }
}
