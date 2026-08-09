-- Targets PostgreSQL. Run this once against an empty database.

CREATE TABLE users (
    user_id    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    post_id      INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      INT          NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    title        VARCHAR(255) NOT NULL,
    content      TEXT         NOT NULL,
    published_at TIMESTAMP    NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comments (
    comment_id INT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id    INT       NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id    INT       NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    review_id   INT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    post_id     INT       NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id     INT       NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    rating      SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT      NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (post_id, user_id)
);

CREATE TABLE tags (
    tag_id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE post_tags (
    post_id INT NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    tag_id  INT NOT NULL REFERENCES tags(tag_id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

-- tags.name and tags.slug are not indexed here separately: their UNIQUE
-- constraints above already created a unique index on each automatically.
CREATE INDEX idx_posts_title   ON posts(title);
CREATE INDEX idx_posts_user_id ON posts(user_id);
