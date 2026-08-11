// A MongoDB schema for Posts with Comments embedded, the NoSQL counterpart
// to posts and comments in schema.sql. See doc/nosql-comparison.md.

db.createCollection("posts", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "title", "content", "createdAt", "updatedAt", "comments"],
      properties: {
        userId: { bsonType: "int" },
        title: { bsonType: "string", maxLength: 255 },
        content: { bsonType: "string" },
        publishedAt: { bsonType: ["date", "null"], description: "null means a draft" },
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

// One post with two comments, shaped like seed.sql's post_id 1.
db.posts.insertOne({
  userId: 1,
  title: "Getting Started with PostgreSQL",
  content: "An introduction to relational databases.",
  publishedAt: new Date("2026-08-01T12:00:00Z"),
  createdAt: new Date("2026-08-01T12:00:00Z"),
  updatedAt: new Date("2026-08-01T12:00:00Z"),
  comments: [
    {
      userId: 2,
      content: "This cleared up a lot of confusion, thank you.",
      createdAt: new Date("2026-08-01T13:00:00Z"),
      updatedAt: new Date("2026-08-01T13:00:00Z")
    },
    {
      userId: 3,
      content: "Would love a follow up on transactions.",
      createdAt: new Date("2026-08-01T14:00:00Z"),
      updatedAt: new Date("2026-08-01T14:00:00Z")
    }
  ]
});

// Adding a comment appends to the array instead of inserting a new row.
db.posts.updateOne(
  { _id: ObjectId("PUT_THE_POST_ID_HERE") },
  {
    $push: { comments: { userId: 4, content: "A new comment.", createdAt: new Date(), updatedAt: new Date() } },
    $set: { updatedAt: new Date() }
  }
);

// One find returns the post and every comment, no join needed.
db.posts.findOne({ _id: ObjectId("PUT_THE_POST_ID_HERE") });
