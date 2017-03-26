package com.bilko;

import spark.Spark;

public class SparkRoutes {

    public static void main(final String[] args) {
        Spark.get("/", ((request, response) -> "Hello World"));
        Spark.get("/test", ((request, response) -> "Hello Test"));
        Spark.get("/echo/:thing", ((request, response) -> request.params(":thing")));
    }
}
