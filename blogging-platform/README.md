
Data layer and persistence stage of a blogging platform. It is a JavaFX
desktop application backed by PostgreSQL, reached through plain JDBC, no ORM. It manages
Posts, Comments, Tags, and Reviews, with searching, sorting, caching, and a measured
performance report included as evidence of the optimizations applied.

## Prerequisites

* Java 11
* Maven
* PostgreSQL, a recent version. I built and verified this against PostgreSQL 18, but
  nothing here depends on a feature newer than PostgreSQL 10, which is where
  `GENERATED ALWAYS AS IDENTITY` was introduced.

**NB**: If you are on **WSL** and plan to run the JavaFX window itself rather than only the
database layer, make sure WSLg is actually rendering windows before troubleshooting this
project specifically. I lost time to a JavaFX window that registered in the taskbar
but never painted anything, and the fix was `wsl --update` followed by `wsl --shutdown`
from Windows.

## Database setup

Create an empty database, then load the schema and the sample data in that order.

```
createdb smart_blog
psql -d smart_blog -f db/schema.sql
psql -d smart_blog -f db/seed.sql
```

[`db/schema.sql`](db/schema.sql) creates every table, key, constraint, and index.
[`db/seed.sql`](db/seed.sql) adds representative sample data, five users, a mix of
published and draft posts, tags, post to tag assignments, comments, and reviews, enough
to exercise every feature without needing to type in your own data first.

## Application configuration

Copy the example properties file and fill in (edit `db.properties` with your own username and password) your own local PostgreSQL credentials.

```
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

## Running the application

```
mvn clean javafx:run
```

This opens a window with two tabs, Posts and Analytics.

Posts are listed with pagination, a search bar lets you search by title or content, by
author, or by tag, all case insensitive, and a sort control lets you reorder the full
matching result set by title or by published date. Sorting is done in
[`util.MergeSort`](src/main/java/util/MergeSort.java) once a non default option is chosen.

The panel on the left manages Tags: create or delete a tag from the full list at any
time, and, once a post is selected, assign an existing tag to it or remove one already
assigned. On the right, a tab per post switches between Comments, where you can add,
edit, and delete comments, and Reviews, where you can add, edit, and delete a 1-5 star
rating with optional text, and see the post's average rating update live.

Analytics runs four read only reports over the live database: the most commented posts,
the highest rated posts, the most used tags, and the most active authors, each a `GROUP
BY` query in [`dao.AnalyticsDao`](src/main/java/dao/AnalyticsDao.java) joining across two
tables. A "Top N" control and a Refresh button are there because this screen holds no
cache of its own, unlike the Posts screen, so it always reflects whatever the database
currently contains; it also refreshes itself automatically whenever you switch to it.

There is no **login screen** in this stage of the project. Anything created through the
application, a new post, comment, or review, is attributed to a fixed seeded user, since
there is no session to attribute it to instead. Tags carry no author and so aren't
affected by this.


## Documentation

* [`doc/database-design.md`](doc/database-design.md) covers the conceptual, logical, and
  physical models, the reasoning behind Reviews existing as its own entity rather than a
  renamed Comment, the constraints a diagram cannot show on its own, the indexing and
  referential integrity decisions, how far each entity's application layer reaches, and
  the aggregate queries behind the Analytics screen.
* [`doc/performance-report.md`](doc/performance-report.md) measures caching and indexing
  against a real database, including a case where an index did not help and an
  explanation of why, rather than only reporting the numbers that looked good.
* [`doc/nosql-comparison.md`](doc/nosql-comparison.md) models Comments as an embedded
  MongoDB document and compares that against the relational design, then explains why
  Reviews, despite looking like the same kind of data, were kept relational instead.
* [`doc/testing-evidence.md`](doc/testing-evidence.md) covers the automated JUnit suite,
  five service classes strong, and the JavaFX interface, verified with real screenshots
  of Tags, Reviews, and Analytics alongside the original Comments evidence.

## Reproducing the performance numbers

[`bench.PerformanceBenchmark`](src/main/java/bench/PerformanceBenchmark.java) produced every number in the performance report. It bulk
inserts synthetic posts and drops and recreates an index inside one transaction, then
rolls that transaction back, so running it does not change your actual data.

```
mvn -q compile
java -cp target/classes:$(find ~/.m2 -name 'postgresql-42.7.4.jar' | head -1) bench.PerformanceBenchmark
```

