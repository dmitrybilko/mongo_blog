package com.bilko;

import java.io.StringWriter;

import java.util.Arrays;
import java.util.HashMap;

import freemarker.template.Configuration;

import spark.Spark;
import spark.utils.StringUtils;

public class SparkFormHandling {

    public static void main(final String[] args) {
        Spark.get("/", ((request, response) -> {
            final StringWriter writer = new StringWriter();
            final Configuration config = new Configuration(Configuration.getVersion());
            config.setClassForTemplateLoading(SparkFormHandling.class, "/");
            config
                .getTemplate("fruitPicker.ftl")
                .process(new HashMap<String, Object>() {
                    {
                        put("fruits", Arrays.asList("apple", "orange", "banana", "peach"));
                    }
                }, writer);
            return writer;
        }));
        Spark.post("/favorite_fruit", ((request, response) -> {
            final String fruit = request.queryParams("fruit");
            if (StringUtils.isEmpty(fruit)) {
                return "Why don't pick one?";
            }
            return "Your favorite fruit is " + fruit;
        }));
    }
}
