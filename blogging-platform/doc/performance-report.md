## Caching

Timed two back-to-back calls to `PostService.listPosts(1, 5)`, the first a cache miss (reaches the database), the second a cache hit (served from the JVM).

```
First call  (cache miss, hits database):  54.708 ms
Second call (cache hit, from memory):      0.029 ms
Speedup: 1867.0x
```

The ~1,900x gap is misleadingly large, and tracing where the 54 ms goes explains why.

Every time `PostDao` runs a query it opens a new database connection, because this project has no connection pool. Opening a connection from scratch is expensive: before a single row is read, the two machines complete a TCP handshake to establish the link and a PostgreSQL authentication handshake to log in and verify credentials.

A cache hit returns data already in memory, so it never contacts the database. That skips both the query and the whole connection setup.

So the honest reading is that the 1,900x figure compares a cache hit against a database call that also pays full connection setup, not against the query alone.

## Indexing

I compared `SELECT post_id, title FROM posts WHERE title = ?` with `idx_posts_title` in place
against the same query with the index dropped, against a table of twenty thousand synthetic
posts. The target title matches exactly one row, a highly selective predicate, which is
exactly the situation an index is meant for.

```
-- With idx_posts_title --
Index Scan using idx_posts_title on posts  (cost=0.29..8.30 rows=1 width=24) (actual time=0.009..0.011 rows=1.00 loops=1)
  Index Cond: ((title)::text = 'Synthetic post 10000'::text)
  Execution Time: 0.018 ms

-- Without idx_posts_title --
Seq Scan on posts  (cost=0.00..932.11 rows=1 width=24) (actual time=1.573..2.278 rows=1.00 loops=1)
  Filter: ((title)::text = 'Synthetic post 10000'::text)
  Rows Removed by Filter: 20008
  Execution Time: 2.296 ms

Average with index:    0.348 ms
Average without index: 2.394 ms
Speedup: 6.9x
```

`EXPLAIN ANALYZE` shows PostgreSQL's own account of what each plan cost, an index scan that
goes straight to the one matching row against a sequential scan that reads and discards
twenty thousand and eight rows to find it. My own averaged timing, one hundred timed calls
through JDBC after three hundred warmup calls, agrees with that in direction, an index scan
is faster, though the two numbers are not expected to match exactly, since my average
includes the JDBC round trip and result set handling on top of what PostgreSQL itself timed.

## What this confirms

Caching removes a full database round trip, connection setup included, on every repeat
request for the same page or search, which is the dominant cost in this project's current
data access pattern. Indexing removes the need to scan every row when a predicate is
selective enough for the planner to prefer it.
