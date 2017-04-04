package com.bilko;

import java.util.Date;

import org.bson.Document;
import org.bson.types.ObjectId;

import static java.util.Arrays.asList;

import static com.bilko.util.Helpers.printJson;

public class DocumentTest {

    public static void main(final String[] args) {
        printJson(
            new Document()
                .append("str", "Mongo, hello")
                .append("int", 42)
                .append("l", 1L)
                .append("double", 1.1)
                .append("b", false)
                .append("date", new Date())
                .append("objectId", new ObjectId())
                .append("null", null)
                .append("embeddedDoc", new Document("x", 0))
                .append("list", asList(1, 2, 3)));
    }
}
