package service;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import dao.PostDao;
import model.Post;
import util.MergeSort;

/**
 * Business rules for posts. No SQL here, PostDao is the only thing in this
 * class that knows a database exists at all.
 */
public class PostService {
    private static final int MIN_TITLE_LENGTH = 5;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MIN_CONTENT_LENGTH = 20;
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final int UNPAGED = Integer.MAX_VALUE;

    /** How a page of posts should be ordered before it is returned. */
    public enum SortOption {
        NEWEST_FIRST, TITLE_A_TO_Z, TITLE_Z_TO_A, PUBLISHED_EARLIEST_FIRST, PUBLISHED_LATEST_FIRST
    }

    private final PostDao postDao;

    // Caches one page of results per query type, term, page number, and page size.
    private final Map<CacheKey, List<Post>> pageCache = new HashMap<>();

    // Caches the full, unpaginated result set per query type and term, used only
    // when a non default sort needs every matching row before it can paginate.
    private final Map<FullResultKey, List<Post>> fullResultCache = new HashMap<>();

    private int cacheHits = 0;
    private int cacheMisses = 0;

    public PostService(PostDao postDao) {
        this.postDao = postDao;
    }

    public Post createPost(Post post) {
        validate(post);
        try {
            if (postDao.existsByAuthorAndTitle(post.getUserId(), post.getTitle(), null)) {
                throw new ValidationException("You already have a post titled \"" + post.getTitle() + "\".");
            }
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
        return listPosts(pageNumber, pageSize, SortOption.NEWEST_FIRST);
    }

    public List<Post> listPosts(int pageNumber, int pageSize, SortOption sortOption) {
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        if (sortOption == SortOption.NEWEST_FIRST) {
            CacheKey key = new CacheKey(QueryType.BROWSE, null, pageNumber, pageSize);
            return getOrLoad(key, () -> {
                try {
                    return postDao.findAll(pageSize, offset);
                } catch (SQLException e) {
                    throw new PostServiceException("Could not load posts.", e);
                }
            });
        }
        List<Post> all = fetchAll(QueryType.BROWSE, null);
        return sortAndPaginate(all, sortOption, offset, pageSize);
    }

    public List<Post> searchByKeyword(String keyword, int pageNumber, int pageSize) {
        return searchByKeyword(keyword, pageNumber, pageSize, SortOption.NEWEST_FIRST);
    }

    public List<Post> searchByKeyword(String keyword, int pageNumber, int pageSize, SortOption sortOption) {
        requireSearchTerm(keyword, "keyword");
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        if (sortOption == SortOption.NEWEST_FIRST) {
            CacheKey key = new CacheKey(QueryType.KEYWORD, keyword, pageNumber, pageSize);
            return getOrLoad(key, () -> {
                try {
                    return postDao.searchByKeyword(keyword, pageSize, offset);
                } catch (SQLException e) {
                    throw new PostServiceException("Could not search posts by keyword.", e);
                }
            });
        }
        List<Post> all = fetchAll(QueryType.KEYWORD, keyword);
        return sortAndPaginate(all, sortOption, offset, pageSize);
    }

    public List<Post> searchByAuthor(String username, int pageNumber, int pageSize) {
        return searchByAuthor(username, pageNumber, pageSize, SortOption.NEWEST_FIRST);
    }

    public List<Post> searchByAuthor(String username, int pageNumber, int pageSize, SortOption sortOption) {
        requireSearchTerm(username, "author");
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        if (sortOption == SortOption.NEWEST_FIRST) {
            CacheKey key = new CacheKey(QueryType.AUTHOR, username, pageNumber, pageSize);
            return getOrLoad(key, () -> {
                try {
                    return postDao.searchByAuthor(username, pageSize, offset);
                } catch (SQLException e) {
                    throw new PostServiceException("Could not search posts by author.", e);
                }
            });
        }
        List<Post> all = fetchAll(QueryType.AUTHOR, username);
        return sortAndPaginate(all, sortOption, offset, pageSize);
    }

    public List<Post> searchByTag(String tagName, int pageNumber, int pageSize) {
        return searchByTag(tagName, pageNumber, pageSize, SortOption.NEWEST_FIRST);
    }

    public List<Post> searchByTag(String tagName, int pageNumber, int pageSize, SortOption sortOption) {
        requireSearchTerm(tagName, "tag");
        int offset = validateAndComputeOffset(pageNumber, pageSize);
        if (sortOption == SortOption.NEWEST_FIRST) {
            CacheKey key = new CacheKey(QueryType.TAG, tagName, pageNumber, pageSize);
            return getOrLoad(key, () -> {
                try {
                    return postDao.searchByTag(tagName, pageSize, offset);
                } catch (SQLException e) {
                    throw new PostServiceException("Could not search posts by tag.", e);
                }
            });
        }
        List<Post> all = fetchAll(QueryType.TAG, tagName);
        return sortAndPaginate(all, sortOption, offset, pageSize);
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
        fullResultCache.clear();
    }

    // Every matching row for a query type and term, unpaginated. Only called when
    // a non default sort is requested, since ordering by title or published date
    // correctly across pages requires seeing every row before slicing out a page.
    private List<Post> fetchAll(QueryType type, String term) {
        FullResultKey key = new FullResultKey(type, term);
        if (fullResultCache.containsKey(key)) {
            cacheHits++;
            return fullResultCache.get(key);
        }
        cacheMisses++;
        List<Post> all;
        try {
            switch (type) {
                case KEYWORD:
                    all = postDao.searchByKeyword(term, UNPAGED, 0);
                    break;
                case AUTHOR:
                    all = postDao.searchByAuthor(term, UNPAGED, 0);
                    break;
                case TAG:
                    all = postDao.searchByTag(term, UNPAGED, 0);
                    break;
                default:
                    all = postDao.findAll(UNPAGED, 0);
            }
        } catch (SQLException e) {
            throw new PostServiceException("Could not load posts to sort.", e);
        }
        fullResultCache.put(key, all);
        return all;
    }

    // Merge sorts the full list by sortOption, then slices out one page of it.
    private List<Post> sortAndPaginate(List<Post> all, SortOption sortOption, int offset, int pageSize) {
        List<Post> sorted = MergeSort.sort(all, comparatorFor(sortOption));
        int from = Math.min(offset, sorted.size());
        int to = Math.min(from + pageSize, sorted.size());
        return sorted.subList(from, to);
    }

    // Draft posts, which have no published_at, always sort to the end either way.
    private Comparator<Post> comparatorFor(SortOption sortOption) {
        switch (sortOption) {
            case TITLE_A_TO_Z:
                return Comparator.comparing(Post::getTitle, String.CASE_INSENSITIVE_ORDER);
            case TITLE_Z_TO_A:
                return Comparator.comparing(Post::getTitle, String.CASE_INSENSITIVE_ORDER).reversed();
            case PUBLISHED_EARLIEST_FIRST:
                return Comparator.comparing(Post::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case PUBLISHED_LATEST_FIRST:
                return Comparator.comparing(Post::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default:
                throw new IllegalArgumentException("No comparator needed for " + sortOption);
        }
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
            if (postDao.existsByAuthorAndTitle(post.getUserId(), post.getTitle(), post.getPostId())) {
                throw new ValidationException("You already have a post titled \"" + post.getTitle() + "\".");
            }
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
        if (post.getTitle().trim().length() < MIN_TITLE_LENGTH) {
            throw new ValidationException("Title must be at least " + MIN_TITLE_LENGTH + " characters long.");
        }
        if (post.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Title cannot be longer than " + MAX_TITLE_LENGTH + " characters.");
        }
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new ValidationException("Content cannot be blank.");
        }
        if (post.getContent().trim().length() < MIN_CONTENT_LENGTH) {
            throw new ValidationException("Content must be at least " + MIN_CONTENT_LENGTH + " characters long.");
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

    // Identifies one cached full, unpaginated result set by query type and term.
    private static final class FullResultKey {
        private final QueryType type;
        private final String term;

        FullResultKey(QueryType type, String term) {
            this.type = type;
            this.term = term;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FullResultKey)) {
                return false;
            }
            FullResultKey that = (FullResultKey) other;
            return type == that.type && Objects.equals(term, that.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, term);
        }
    }
}
