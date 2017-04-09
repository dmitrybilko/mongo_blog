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

package com.bilko.dao;

import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.util.Random;

import sun.misc.BASE64Encoder;

import org.bson.Document;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class UserDao {

    private final MongoCollection<Document> collection;
    private Random random = new SecureRandom();

    public UserDao(final MongoDatabase db) {
        collection = db.getCollection("users");
    }

    public boolean addUser(final String username, final String password, final String email) {
        final Document user =
            new Document()
                .append("_id", username)
                .append("password", makePasswordHash(password, Integer.toString(random.nextInt())));

        if (email != null && !email.equals("")) {
            user.append("email", email);
        }

        try {
            collection.insertOne(user);
            return true;
        } catch (final MongoWriteException e) {
            if (e.getError().getCategory().equals(ErrorCategory.DUPLICATE_KEY)) {
                System.out.println("USERNAME ALREADY IN USE: " + username);
                return false;
            }
            throw e;
        }
    }

    public Document validateLogin(final String username, final String password) {
        final Document user =
            collection
                .find(new Document("_id", username))
                .first();

        if (user == null) {
            System.out.println("USER NOT IN DB");
            return null;
        }

        final String hashedAndSalted = user.get("password").toString();
        if (!hashedAndSalted.equals(makePasswordHash(password, hashedAndSalted.split(",")[1]))) {
            System.out.println("SUBMITTED PASSWORD IS NOT A MATCH");
            return null;
        }

        return user;
    }

    private String makePasswordHash(final String password, final String salt) {
        final String saltedAndHashed = password + "," + salt;
        try {
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(saltedAndHashed.getBytes());
            final byte hashedBytes[] = new String(digest.digest(), "UTF-8").getBytes();
            return new BASE64Encoder().encode(hashedBytes) + "," + salt;
        } catch (final NoSuchAlgorithmException nsae) {
            throw new RuntimeException("MD5 IS NOT AVAILABLE", nsae);
        } catch (final UnsupportedEncodingException uee) {
            throw new RuntimeException("UTF-8 UNAVAILABLE? NOT A CHANCE", uee);
        }
    }
}
