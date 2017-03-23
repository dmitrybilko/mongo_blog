package com.bilko;

import spark.Spark;

public class HelloWorldSparkStyle {

    public static void main(final String[] args) {
        Spark.get("/", ((request, response) -> "Hello World from Spark"));
    }
}