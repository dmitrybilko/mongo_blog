package com.bilko;

import java.io.StringWriter;

import java.util.HashMap;

import spark.Spark;

import freemarker.template.Configuration;

public class HelloWorldSparkFreemarkerStyle {

    public static void main(final String[] args) {
        Spark.get("/", ((request, response) -> {
            final StringWriter writer = new StringWriter();
            final Configuration config = new Configuration(Configuration.getVersion());
            config.setClassForTemplateLoading(HelloWorldSparkFreemarkerStyle.class, "/");
            config
                .getTemplate("hello.ftl")
                .process(new HashMap<String, Object>() {
                    {
                        put("name1", "Freemarker");
                        put("name2", "Spark");
                    }
                }, writer);
            return writer;
        }));
    }
}
