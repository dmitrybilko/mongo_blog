/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bilko.controller;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

import spark.Request;
import spark.Response;
import spark.Route;

import freemarker.template.Template;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;

import com.bilko.dao.BlogPostDao;
import com.bilko.dao.SessionDao;
import com.bilko.dao.UserDao;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * This class encapsulates the controllers for the blog web application. It delegates all interaction with MongoDB
 * to three Data Access Objects (DAOs). It is also the entry point into the web application.
 */
public class BlogController {

    private final Configuration config;
    private final UserDao userDao;
    private final SessionDao sessionDao;
    private final BlogPostDao blogPostDao;

    private BlogController(final String uri) throws IOException {
        final MongoDatabase db = new MongoClient(new MongoClientURI(uri)).getDatabase("blog");

        config = createFreemarkerConfiguration();
        userDao = new UserDao(db);
        sessionDao = new SessionDao(db);
        blogPostDao = new BlogPostDao(db);

        port(8082);
        initRoutes();
    }

    public static void main(final String[] args) throws IOException {
        if (args.length == 0) {
            new BlogController("mongodb://localhost");
        } else {
            new BlogController(args[0]);
        }
    }

    private void initRoutes() throws IOException {

        get("/", new FreemarkerBasedRoute("blog_template.ftl") {

            @Override
            public void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String username = sessionDao.findUserNameBySessionId(getSessionCookie(request));
                final List<Document> posts = blogPostDao.findByDateDescending(10);
                final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                if (StringUtils.isBlank(username)) {
                    root.put("username", username);
                }
                root.put("myposts", posts);

                template.process(root, writer);
            }
        });

        get("/welcome", new FreemarkerBasedRoute("welcome.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String username = sessionDao.findUserNameBySessionId(getSessionCookie(request));
                if (StringUtils.isBlank(username)) {
                    System.out.println("welcome() CAN'T IDENTIFY THE USER, REDIRECTING TO signup");
                    response.redirect("/signup");
                } else {
                    final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    root.put("username", username);
                    template.process(root, writer);
                }
            }
        });

        get("/signup", new FreemarkerBasedRoute("signup.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                root.put("username", "");
                root.put("password", "");
                root.put("email", "");
                root.put("password_error", "");
                root.put("username_error", "");
                root.put("email_error", "");
                root.put("verify_error", "");

                template.process(root, writer);
            }
        });

        post("/signup", new FreemarkerBasedRoute("signup.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String email = request.queryParams("email");
                final String username = request.queryParams("username");
                final String password = request.queryParams("password");
                final String verify = request.queryParams("verify");
                final HashMap<String, String> root = new HashMap<String, String>() {
                    {
                        put("username", StringEscapeUtils.escapeHtml4(username));
                        put("email", StringEscapeUtils.escapeHtml4(email));
                    }
                };

                if (validateSignup(username, password, verify, email, root)) {
                    System.out.println("SIGNUP: CREATING USER WITH: " + username + " " + password);
                    if (!userDao.addUser(username, password, email)) {
                        root.put("username_error", "USERNAME ALREADY IN USE, PLEASE CHOOSE ANOTHER");
                        template.process(root, writer);
                    } else {
                        final String sessionId = sessionDao.startSession(username);
                        System.out.println("SESSION ID IS: " + sessionId);
                        response.raw().addCookie(new Cookie("session", sessionId));
                        response.redirect("/welcome");
                    }
                } else {
                    System.out.println("USER REGISTRATION DIDN'T VALIDATE");
                    template.process(root, writer);
                }
            }
        });

        get("/login", new FreemarkerBasedRoute("login.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                root.put("username", "");
                root.put("login_error", "");
                template.process(root, writer);
            }
        });

        post("/login", new FreemarkerBasedRoute("login.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String username = request.queryParams("username");
                final String password = request.queryParams("password");
                final Document user = userDao.validateLogin(username, password);
                System.out.println("LOGIN: USER SUBMITTED: " + username + " " + password);

                if (user != null) {
                    final String sessionId = sessionDao.startSession(user.get("_id").toString());
                    if (sessionId == null) {
                        response.redirect("/internal_error");
                    } else {
                        response.raw().addCookie(new Cookie("session", sessionId));
                        response.redirect("/welcome");
                    }
                } else {
                    final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    root.put("username", StringEscapeUtils.escapeHtml4(username));
                    root.put("password", "");
                    root.put("login_error", "Invalid Login");
                    template.process(root, writer);
                }
            }
        });

        get("/newpost", new FreemarkerBasedRoute("newpost_template.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String username = sessionDao.findUserNameBySessionId(getSessionCookie(request));
                if (StringUtils.isBlank(username)) {
                    response.redirect("/login");
                } else {
                    SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    root.put("username", username);
                    template.process(root, writer);
                }
            }
        });

        post("/newpost", new FreemarkerBasedRoute("newpost_template.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String title = StringEscapeUtils.escapeHtml4(request.queryParams("subject"));
                final String post = StringEscapeUtils.escapeHtml4(request.queryParams("body"));
                final String tags = StringEscapeUtils.escapeHtml4(request.queryParams("tags"));
                final String username = sessionDao.findUserNameBySessionId(getSessionCookie(request));
                if (StringUtils.isBlank(username)) {
                    response.redirect("/login");
                } else if (StringUtils.isBlank(title) || StringUtils.isBlank(post)) {
                    HashMap<String, String> root = new HashMap<String, String>() {
                        {
                            put("errors", "post must contain a title and blog entry.");
                            put("subject", title);
                            put("username", username);
                            put("tags", tags);
                            put("body", post);
                        }
                    };
                    template.process(root, writer);
                } else {
                    final String permalink = blogPostDao.addPost(
                        title, post.replaceAll("\\r?\\n", "<p>"), extractTags(tags), username);
                    response.redirect("/post/" + permalink);
                }
            }
        });

        get("/post/:permalink", new FreemarkerBasedRoute("entry_template.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String permalink = request.params(":permalink");
                System.out.println("/post: GET " + permalink);

                final Document post = blogPostDao.findByPermalink(permalink);
                if (post == null) {
                    response.redirect("/post_not_found");
                } else {
                    final SimpleHash comment = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    comment.put("name", "");
                    comment.put("email", "");
                    comment.put("body", "");

                    final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    root.put("post", post);
                    root.put("comments", comment);

                    template.process(root, writer);
                }
            }
        });

        post("/newcomment", new FreemarkerBasedRoute("entry_template.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String name = StringEscapeUtils.escapeHtml4(request.queryParams("commentName"));
                final String email = StringEscapeUtils.escapeHtml4(request.queryParams("commentEmail"));
                final String body = StringEscapeUtils.escapeHtml4(request.queryParams("commentBody"));
                final String permalink = request.queryParams("permalink");
                final Document post = blogPostDao.findByPermalink(permalink);
                if (post == null) {
                    response.redirect("/post_not_found");
                } else if (StringUtils.isBlank(name) || StringUtils.isBlank(body)) {
                    final SimpleHash comment = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    comment.put("name", name);
                    comment.put("email", email);
                    comment.put("body", body);

                    final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                    root.put("comments", comment);
                    root.put("post", post);
                    root.put("errors", "POST MUST CONTAIN YOUR NAME AND AN ACTUAL COMMENT");

                    template.process(root, writer);
                } else {
                    blogPostDao.addPostComment(name, email, body, permalink);
                    response.redirect("/post/" + permalink);
                }
            }
        });

        get("/logout", new FreemarkerBasedRoute("signup.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String sessionId = getSessionCookie(request);
                if (sessionId == null) {
                    response.redirect("/login");
                } else {
                    sessionDao.endSession(sessionId);
                    final Cookie cookie = getSessionCookieActual(request);
                    if (cookie != null) {
                        cookie.setMaxAge(0);
                        response.raw().addCookie(cookie);
                        response.redirect("/login");
                    }
                }
            }
        });

        get("/post_not_found", new FreemarkerBasedRoute("post_not_found.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                template.process(new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion())), writer);
            }
        });

        get("/internal_error", new FreemarkerBasedRoute("error_template.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final SimpleHash root = new SimpleHash(new DefaultObjectWrapper(Configuration.getVersion()));
                root.put("error", "SYSTEM HAS ENCOUNTERED AN ERROR");
                template.process(root, writer);
            }
        });
    }

    private String getSessionCookie(final Request request) {
        final Cookie[] cookies = request.raw().getCookies();
        if (cookies == null) {
            return null;
        }
        for (final Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private Cookie getSessionCookieActual(final Request request) {
        final Cookie[] cookies = request.raw().getCookies();
        if (cookies == null) {
            return null;
        }
        for (final Cookie cookie : cookies) {
            if (cookie.getName().equals("session")) {
                return cookie;
            }
        }
        return null;
    }

    private boolean validateSignup(final String username, final String password, final String verify,
        final String email, final HashMap<String, String> errors) {
        errors.put("username_error", "");
        errors.put("password_error", "");
        errors.put("verify_error", "");
        errors.put("email_error", "");

        if (!username.matches("^[a-zA-Z0-9_-]{3,20}$")) {
            errors.put("username_error", "INVALID USERNAME. TRY JUST LETTERS AND NUMBERS");
            return false;
        }

        if (!password.matches("^.{3,20}$")) {
            errors.put("password_error", "INVALID PASSWORD");
            return false;
        }

        if (!password.equals(verify)) {
            errors.put("verify_error", "PASSWORD MUST MATCH");
            return false;
        }

        if (!email.equals("")) {
            if (!email.matches("^[\\S]+@[\\S]+\\.[\\S]+$")) {
                errors.put("email_error", "INVALID EMAIL ADDRESS");
                return false;
            }
        }

        return true;
    }

    private Configuration createFreemarkerConfiguration() {
        final Configuration config = new Configuration(Configuration.getVersion());
        config.setClassForTemplateLoading(BlogController.class, "/");
        return config;
    }

    private ArrayList<String> extractTags(final String tags) {
        final String tagArray[] =
            tags
                .replaceAll("\\s", "")
                .split(",");

        final ArrayList<String> cleaned = new ArrayList<>();
        for (final String tag : tagArray) {
            if (StringUtils.isNotBlank(tag) && !cleaned.contains(tag)) {
                cleaned.add(tag);
            }
        }
        return cleaned;
    }

    private abstract class FreemarkerBasedRoute implements Route {

        final Template template;

        private FreemarkerBasedRoute(final String templateName) throws IOException {
            template = config.getTemplate(templateName);
        }

        @Override
        public Object handle(final Request request, final Response response) {
            final StringWriter writer = new StringWriter();
            try {
                doHandle(request, response, writer);
            } catch (IOException | TemplateException e) {
                e.printStackTrace();
                response.redirect("/internal_error");
            }
            return writer;
        }

        protected abstract void doHandle(final Request request, final Response response, final Writer writer)
            throws IOException, TemplateException;
    }
}
