# Testing Evidence

I have two layers of evidence here. Automated JUnit tests cover the Service and DAO
layers, and screenshots cover the JavaFX layer, since a plain JUnit test never opens a
window and cannot click a button.

## Automated tests

`src/test/java/service/PostServiceTest.java` and `CommentServiceTest.java` run against
the real PostgreSQL database this application actually uses, the same one configured in
`db.properties`, not a mock. That was a deliberate choice, consistent with how every
feature in this project was verified while building it, real SQL, real constraints, real
query results, not a simulation of them.

Every test that writes a row deletes it again before finishing. I ran the suite twice in a
row and confirmed zero leftover rows afterward, so it can be run repeatedly without
drifting the seed data other tests, or the application itself, rely on.

Running `mvn test` gives this:

```
[INFO] Running service.PostServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.778 s -- in service.PostServiceTest
[INFO] Running service.CommentServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.286 s -- in service.CommentServiceTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] BUILD SUCCESS
```

Between the two classes, this covers creating, reading, updating, and deleting both
posts and comments, validation rejecting blank or oversized input, a foreign key
violation being translated into a readable message, search returning the right rows
case insensitively, sorting by title and by published date, drafts sorting last, and the
cache correctly recording a miss on the first call, a hit on the second, and a fresh miss
again after a write invalidates it.

Running `mvn test` requires a configured `db.properties`, the same requirement as running
the application itself.

## JavaFX interface

![Selecting a post loads its real comments from the database into the Comments panel](images/comments-panel-loaded.png)

Selecting "Getting Started with PostgreSQL" loads its two seeded comments into the panel
on the right, confirming `PostController` and `CommentController` are wired together
correctly through `fx:include`.

![Adding a comment through the text field and Add button appends it and confirms with a status message](images/comments-panel-after-add.png)

Typing into the comment field and pressing Add appends the new comment and shows
"Comment added.", confirming the create path works end to end, from a real button click
down to a real row in the database. I then deleted that comment the same way and
confirmed the database returned to its original state, so this check left no residue.
