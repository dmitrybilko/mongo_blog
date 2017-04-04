package com.bilko;

import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;

public class App {

    public static void main(final String[] args) {
        new MongoClient()
            .getDatabase("test")
            .withReadPreference(ReadPreference.secondary())
            .getCollection("test");
    }
}
