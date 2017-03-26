package com.bilko;

import java.io.IOException;
import java.io.StringWriter;

import java.util.HashMap;

import spark.Spark;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class HelloWorldSparkFreemarkerStyle {

    public static void main(final String[] args) throws IOException, TemplateException {
        Spark.get("/", ((request, response) -> {

            final StringWriter writer = new StringWriter();
            final Configuration config = new Configuration(Configuration.getVersion());
            config.setClassForTemplateLoading(HelloWorldFreemarkerStyle.class, "/");
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
