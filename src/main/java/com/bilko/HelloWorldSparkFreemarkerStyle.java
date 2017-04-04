package com.bilko;

import java.io.StringWriter;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;

import spark.Spark;

import freemarker.template.Configuration;

public class HelloWorldSparkFreemarkerStyle {

    public static void main(final String[] args) {
        final MongoCollection<Document> collection =
            new MongoClient()
                .getDatabase("course")
                .getCollection("hello");
        collection.drop();
        collection.insertOne(new Document("name1", "Freemarker").append("name2", "Spark"));

        Spark.get("/", ((request, response) -> {
            final StringWriter writer = new StringWriter();
            final Configuration config = new Configuration(Configuration.getVersion());
            config.setClassForTemplateLoading(HelloWorldSparkFreemarkerStyle.class, "/");
            config
                .getTemplate("hello.ftl")
                .process(collection.find().first(), writer);
            return writer;
        }));
    }
}
