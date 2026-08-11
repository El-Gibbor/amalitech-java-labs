# NoSQL Comparison

I modeled Comments as a document schema, embedded inside their Post rather than kept as a
separate collection. The schema itself is in `db/comments-nosql-schema.js`.

## Justification

Comments are almost always read together with their post, never on their own. Every place
this application touches a comment, `CommentController` loading them when a post is
selected, `getCommentsForPost` in `CommentService`, is really asking one question: show me
this post together with what has been said about it. Embedding Comments inside their post
document answers that with a single read, no join, which matches how this application
actually uses them.

### Scope: flat comments, not threaded

The spec asks for comment management (User Story 2.1), not threaded replies. I designed it
flat: a flat `comments` table in SQL, a flat `comments` array here, and a JavaFX UI that adds
a comment to a post, not to another comment. Had threaded replies been required, the document
model's advantage would widen: nesting stores arbitrary reply depth in a single read. The
relational model, by contrast, would need a self-referencing `parent_comment_id` on the
`comments` table and a recursive query (`WITH RECURSIVE`) to reassemble a thread from rows
connected only by pointers.


## The schema

```javascript
db.createCollection("posts", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "title", "content", "createdAt", "updatedAt", "comments"],
      properties: {
        userId: { bsonType: "int" },
        title: { bsonType: "string", maxLength: 255 },
        content: { bsonType: "string" },
        publishedAt: { bsonType: ["date", "null"] },
        createdAt: { bsonType: "date" },
        updatedAt: { bsonType: "date" },
        comments: {
          bsonType: "array",
          items: {
            bsonType: "object",
            required: ["userId", "content", "createdAt"],
            properties: {
              userId: { bsonType: "int" },
              content: { bsonType: "string", maxLength: 2000 },
              createdAt: { bsonType: "date" },
              updatedAt: { bsonType: "date" }
            }
          }
        }
      }
    }
  }
});
```

`db/comments-nosql-schema.js` also has a representative document and the two operations
that matter most, adding a comment and reading a post with its comments.

## SQL versus NoSQL

| | Relational, `comments` table | Document, embedded in `posts` |
|---|---|---|
| Reading a post with its comments | A join between `posts` and `comments` | One document read, no join |
| Adding a comment | An `INSERT` into a separate table | An array push into the post document |
| Enforcing a comment belongs to a real post | `FOREIGN KEY ... ON DELETE CASCADE` links and cleans up comments | Guaranteed by structure: a comment exists only inside its post, so it cannot outlive one |
| Querying comments on their own, across posts | Direct and indexable, `SELECT * FROM comments WHERE user_id = ?` | Expensive: no independent comment to index, so the query scans every post and scales with total comment volume |
| Best suited to | Data queried and combined on its own terms | Data almost always read and written together with one parent |