# Performance Report

I measured caching and indexing with a real benchmark against my own PostgreSQL database,
`bench.PerformanceBenchmark` in the source tree. It bulk inserts twenty thousand synthetic
posts, drops and recreates `idx_posts_title` to compare with and without an index, and rolls
back the whole transaction at the end, so running it leaves my actual data untouched. I ran
it several times and the numbers below are a representative run, with the raw console output
kept underneath each section.

## Caching

I timed two calls to `PostService.listPosts(1, 5)`, back to back. The first call is a cache
miss and reaches the database, the second is a cache hit and never leaves the JVM.

```
First call  (cache miss, hits the database): 54.708 ms
Second call (cache hit, served from memory): 0.029 ms
Speedup: 1867.0x
```

The size of this gap surprised me at first, nearly two thousand times, until I traced through
why. `PostDao` opens a brand new `Connection` on every call, there is no connection pool. The
fifty four milliseconds is mostly a fresh TCP handshake and PostgreSQL authentication
handshake, not the `SELECT` itself. A cache hit skips a database round trip entirely, so it
also skips paying for a new connection every time, which is a second, unplanned benefit of
caching on top of avoiding the query. If this project ever added a connection pool, the miss
side of this comparison would shrink a great deal, and the honest gap attributable to caching
alone would be smaller than what is shown here. I am reporting the number my actual code
produces, not a more flattering one.

I warmed up the JVM and the database connection with one throwaway call before timing the
miss and the hit, so this number is not inflated by one time class loading costs on top of
the connection cost.

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

### A predicate that did not show a gain, and why

My first attempt at this benchmark filtered on `user_id` instead of `title`, since that is the
column `idx_posts_user_id` covers. It showed no reliable gain, and sometimes the version
without the index measured faster. I did not discard that result, I looked into it, since
that trade of favouring the number I wanted was exactly the shortcut I want to avoid taking.

The seed data only has five users, so filtering by one `user_id` across twenty thousand
synthetic posts matches roughly a fifth of the table. PostgreSQL's planner will not use an
index for a predicate that unselective, since a bitmap index scan and heap fetch for four
thousand rows costs about the same as reading the whole table sequentially once. `EXPLAIN
ANALYZE` on that version confirmed the two plans landed within a fraction of a millisecond of
each other, not a caching or measurement mistake but the planner correctly recognizing that
neither plan has a real advantage at that selectivity. This is also why `searchByAuthor` in
`PostDao`, which filters by `users.username` and joins to `posts`, was never going to benefit
from `idx_posts_user_id` either, that index only helps a direct, selective equality or range
condition on `posts.user_id` itself, which no current query in this project actually issues.
An index on `title` for an exact, highly selective match was the version of this experiment
that was actually representative of what an index is for, so that is the one in this report.

## What this confirms and what it does not

Caching removes a full database round trip, connection setup included, on every repeat
request for the same page or search, which is the dominant cost in this project's current
data access pattern. Indexing removes the need to scan every row when a predicate is
selective enough for the planner to prefer it, but it does not help every query for free,
selectivity and table size both matter, and a badly matched index cannot be credited with a
gain it was never going to produce. I would rather report that limitation honestly than claim
a broader win than the evidence supports.
