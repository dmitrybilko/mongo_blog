package com.bilko;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

import static java.util.Arrays.asList;

public class InsertTest {

    public static void main(final String[] args) {
        final MongoCollection<Document> collection =
            new MongoClient()
                .getDatabase("course")
                .getCollection("insertTest");
        collection.drop();
        final Document smith =
            new Document()
                .append("name", "Smith")
                .append("age", 30)
                .append("profession", "programmer");
        final Document jones =
            new Document()
                .append("name", "Jones")
                .append("age", 25)
                .append("profession", "hacker");
        collection.insertMany(asList(smith, jones));
        collection.insertOne(smith);
    }
}
