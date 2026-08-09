-- Representative sample data.
-- Run this once, immediately after schema.sql, against an empty database.
-- The literal ids below rely on identity columns starting at 1 in insertion
-- order, so this only produces the ids I reference if the tables are empty
-- before it runs.

INSERT INTO users (username, email) VALUES
    ('jane_writer', 'jane@example.com'),   -- user_id 1
    ('mike_dev',    'mike@example.com'),   -- user_id 2
    ('sara_reads',  'sara@example.com'),   -- user_id 3
    ('tomlogs',     'tom@example.com'),    -- user_id 4
    ('amy_codes',   'amy@example.com');    -- user_id 5

INSERT INTO posts (user_id, title, content, published_at) VALUES
    (1, 'Getting Started with PostgreSQL', 'An introduction to relational databases.', CURRENT_TIMESTAMP),   -- post_id 1
    (1, 'Why Indexes Matter',               'A look at how a B-tree index speeds up search.', CURRENT_TIMESTAMP), -- post_id 2
    (2, 'JavaFX for Beginners',             'Building a first desktop interface.', CURRENT_TIMESTAMP),       -- post_id 3
    (2, 'Draft: Thoughts on JDBC',          'Unfinished notes, not published yet.', NULL),                   -- post_id 4, a draft
    (3, 'Book Review Roundup',              'My opinion on a few recent reads.', CURRENT_TIMESTAMP),         -- post_id 5
    (4, 'A Post With No Tags Or Comments',  'Deliberately left isolated for testing edge cases.', CURRENT_TIMESTAMP), -- post_id 6
    (5, 'The Most Popular Post',            'A post with heavy engagement, for testing sorting and pagination.', CURRENT_TIMESTAMP); -- post_id 7

INSERT INTO tags (name, slug) VALUES
    ('Databases', 'databases'),   -- tag_id 1
    ('Beginner',  'beginner'),    -- tag_id 2
    ('PostgreSQL','postgresql'),  -- tag_id 3
    ('JavaFX',    'javafx'),      -- tag_id 4
    ('Java',      'java'),        -- tag_id 5
    ('Opinion',   'opinion');     -- tag_id 6

INSERT INTO post_tags (post_id, tag_id) VALUES
    (1, 1), (1, 2), (1, 3),                  -- Getting Started with PostgreSQL
    (2, 1), (2, 3),                          -- Why Indexes Matter
    (3, 4), (3, 5), (3, 2),                  -- JavaFX for Beginners
    (4, 5),                                  -- Draft: Thoughts on JDBC
    (5, 6),                                  -- Book Review Roundup
    -- post_id 6 intentionally has no tags
    (7, 1), (7, 4), (7, 5), (7, 2), (7, 6);  -- The Most Popular Post

INSERT INTO comments (post_id, user_id, content) VALUES
    (1, 2, 'This cleared up a lot of confusion, thank you.'),
    (1, 3, 'Would love a follow up on transactions.'),
    (3, 4, 'JavaFX still feels underrated.'),
    (7, 1, 'Great writeup, learned a lot.'),
    (7, 2, 'Disagree with the last section, but well argued.'),
    (7, 3, 'Bookmarking this.'),
    (7, 4, 'One of the better posts this month.');
    -- post_id 6 intentionally has no comments

INSERT INTO reviews (post_id, user_id, rating, review_text) VALUES
    (1, 2, 5, 'Clear and well paced.'),
    (3, 3, 4, NULL),
    (7, 1, 5, 'Excellent depth.'),
    (7, 2, 4, 'Very good, a little long.'),
    (7, 3, 3, 'Solid but not groundbreaking.');
    -- post_id 6 intentionally has no reviews
