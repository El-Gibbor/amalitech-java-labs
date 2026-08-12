package model;

import java.time.LocalDateTime;

/** A plain data holder mirroring one row of the tags table. No SQL, no validation. */
public class Tag {
    private Integer tagId;
    private String name;
    private String slug;
    private LocalDateTime createdAt;

    public Tag() {
    }

    public Tag(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
