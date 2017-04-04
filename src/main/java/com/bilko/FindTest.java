package com.bilko;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import static com.bilko.util.Helpers.printJson;

public class FindTest {

    public static void main(final String[] args) {
        final MongoCollection<Document> collection =
            new MongoClient()
                .getDatabase("course")
                .getCollection("findTest");
        collection.drop();

        for (int i = 0; i < 10; i++) {
            collection.insertOne(new Document("i", i));
        }

        final List<Document> documents = collection.find().into(new ArrayList<>());
        for (final Document doc : documents) {
            printJson(doc);
        }

        try (final MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                printJson(cursor.next());
            }
        }
    }
}
