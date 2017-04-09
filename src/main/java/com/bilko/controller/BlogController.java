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

import java.util.HashMap;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringEscapeUtils;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import spark.Request;
import spark.Response;
import spark.Route;

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

    private BlogController(final String uri) throws IOException {
        final MongoDatabase db = new MongoClient(new MongoClientURI(uri)).getDatabase("blog");

        userDao = new UserDao(db);
        sessionDao = new SessionDao(db);
        config = createFreemarkerConfiguration();

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

               // this is where we would normally load up the blog data, but this week, we just display a placeholder.
                template.process(new HashMap<>(), writer);
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

                final HashMap<String, String> root = new HashMap<>();
                root.put("username", StringEscapeUtils.escapeHtml4(username));
                root.put("email", StringEscapeUtils.escapeHtml4(email));

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

        get("/signup", new FreemarkerBasedRoute("signup.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final SimpleHash root = new SimpleHash();
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

        get("/welcome", new FreemarkerBasedRoute("welcome.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final String username = sessionDao.findUserNameBySessionId(getSessionCookie(request));
                if (username == null) {
                    System.out.println("welcome() CAN'T IDENTIFY THE USER, REDIRECTING TO signup");
                    response.redirect("/signup");
                } else {
                    final SimpleHash root = new SimpleHash();
                    root.put("username", username);
                    template.process(root, writer);
                }
            }
        });

        get("/login", new FreemarkerBasedRoute("login.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final SimpleHash root = new SimpleHash();
                root.put("username", "");
                root.put("login_error", "");
                template.process(root, writer);
            }
        });

        // Process output coming from login form. On success redirect folks to the welcome page on failure,
        // just return an error and let them try again.
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
                    final SimpleHash root = new SimpleHash();
                    root.put("username", StringEscapeUtils.escapeHtml4(username));
                    root.put("password", "");
                    root.put("login_error", "Invalid Login");
                    template.process(root, writer);
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

        get("/internal_error", new FreemarkerBasedRoute("error_template.ftl") {

            @Override
            protected void doHandle(final Request request, final Response response, final Writer writer)
                throws IOException, TemplateException {

                final SimpleHash root = new SimpleHash();
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
        config.setClassForTemplateLoading(BlogController.class, "/freemarker");
        return config;
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
