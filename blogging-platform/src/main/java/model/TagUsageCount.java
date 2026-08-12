package model;

/**
 * One row of the "most used tags" analytics report. This is a query result,
 * not a table row, so unlike Tag it is immutable and has no setters.
 */
public class TagUsageCount {
    private final String tagName;
    private final int postCount;

    public TagUsageCount(String tagName, int postCount) {
        this.tagName = tagName;
        this.postCount = postCount;
    }

    public String getTagName() {
        return tagName;
    }

    public int getPostCount() {
        return postCount;
    }
}
