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

import java.security.SecureRandom;

import sun.misc.BASE64Encoder;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static com.mongodb.client.model.Filters.eq;

public class SessionDao {

    private final MongoCollection<Document> collection;

    public SessionDao(final MongoDatabase db) {
        collection = db.getCollection("sessions");
    }

    public String findUserNameBySessionId(final String sessionId) {
        final Document session = getSession(sessionId);
        if (session == null) {
            return null;
        } else {
            return session.get("username").toString();
        }
    }

    public String startSession(final String username) {
        final byte randomBytes[] = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        final Document session =
            new Document("username", username).append("_id", new BASE64Encoder().encode(randomBytes));
        collection.insertOne(session);
        return session.getString("_id");
    }

    public void endSession(final String sessionId) {
        collection.deleteOne(eq("_id", sessionId));
    }

    private Document getSession(final String sessionId) {
        return collection
            .find(eq("_id", sessionId))
            .first();
    }
}
