package com.bilko;

import java.io.IOException;
import java.io.StringWriter;

import java.util.HashMap;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class HelloWorldFreemarkerStyle {

    public static void main(final String[] args) throws IOException, TemplateException {
        final StringWriter writer = new StringWriter();
        final Configuration config = new Configuration(Configuration.getVersion());
        config.setClassForTemplateLoading(HelloWorldFreemarkerStyle.class, "/");
        config
            .getTemplate("hello.ftl")
            .process(new HashMap<String, Object>() {
                {
                    put("name", "Freemarker");
                }
            }, writer);
        System.out.println(writer);
    }
}
