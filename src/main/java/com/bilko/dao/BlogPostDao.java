package com.bilko.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.push;

public class BlogPostDao {

    private final MongoCollection<Document> posts;

    public BlogPostDao(final MongoDatabase db) {
        posts = db.getCollection("posts");
    }

    public Document findByPermalink(final String permalink) {
        return posts.find(eq("permalink", permalink)).first();
    }

    public List<Document> findByDateDescending(final int limit) {
        return posts
            .find()
            .sort(descending("date"))
            .limit(limit)
            .into(new ArrayList<>());
    }


    public String addPost(final String title, final String body, final List tags, final String username) {
        System.out.println("INSERTING BLOG ENTRY WITH TITLE [" + title + "] AND BODY [" + body + "]");

        final String permalink =
            title
                .replaceAll("\\s", "_")
                .replaceAll("\\W", "")
                .toLowerCase();

        posts.insertOne(
            new Document()
                .append("title", title)
                .append("author", username)
                .append("body", body)
                .append("permalink", permalink)
                .append("tags", tags)
                .append("comments", new ArrayList<>())
                .append("date", new Date())
        );

        return permalink;
    }

    public void addPostComment(final String name, final String email, final String body, final String permalink) {
        final Document comment =
            new Document()
                .append("author", name)
                .append("body", body);

        if (StringUtils.isNotEmpty(email)) {
            comment.append("email", email);
        }

        posts.updateOne(eq("permalink", permalink), push("comments", comment));
    }
}
