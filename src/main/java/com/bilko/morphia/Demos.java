package com.bilko.morphia;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import static org.junit.Assert.assertEquals;

//public class Demos extends BaseTest {
public class Demos {

    private SimpleDateFormat sdf = new SimpleDateFormat("DD-mm-yyyy");
    private GithubUser gotoalice;
    private Date date;

    public Demos() throws ParseException {
        date = sdf.parse("22-09-1988");
    }

    @Test
    public void basicUser() {
        gotoalice = new GithubUser("gotoalice");
        gotoalice.fullName = "Dmitry Bilko";
        gotoalice.memberSince = date;
        gotoalice.following = 1000;

//        ds.save(gotoalice);
    }

//    @Test(dependsOnMethods = {"basicUser"})
    public void repositories() throws ParseException {
        final Organization org = new Organization("mongodb");
//        ds.save(org);

        final Repository morphia1 = new Repository(org, "morphia");
        final Repository morphia2 = new Repository(gotoalice, "morphia");

        gotoalice.repositories.add(morphia1);
        gotoalice.repositories.add(morphia2);

//        ds.save(gotoalice);
    }

//    @Test(dependsOnMethods = {"repositories"})
    public void query() {
//        final Query<Repository> query = ds.createQuery(Repository.class);
//        final Repository repo = query.get();
//        final List<Repository> repositories = query.asList();
//        final Iterable<Repository> fetch = query.fetch();
//        ((MorphiaIterator) fetch).close();

//        Iterator<Repository> iterator = fetch.iterator();
//        while (iterator.hasNext()) {
//            iterator.next();
//        }

//        iterator = fetch.iterator();
//        while (iterator.hasNext()) {
//            iterator.next();
//        }

//        query
//            .field("owner")
//            .equal(gotoalice)
//            .get();

//        final GithubUser memberSince =
//            ds
//                .createQuery(GithubUser.class)
//                .field("memberSince")
//                .equal(date)
//                .get();
//        System.out.println("memberSince = " + memberSince);

//        final GithubUser since =
//            ds
//                .createQuery(GithubUser.class)
//                .field("since")
//                .equal(date)
//                .get();
//        System.out.println("since = " + memberSince);
    }

//    @Test(dependsOnMethods = {"repositories"})
    public void updates() {
        gotoalice.followers = 12;
        gotoalice.following = 678;

//        ds.save(gotoalice);
    }

//    @Test(dependsOnMethods = {"repositories"})
    public void massUpdates() {
//        final UpdateOperations<GithubUser> update =
//            ds
//                .createUpdateOperations(GithubUser.class)
//                .inc("followers")
//                .set("following", 42);
//        final Query<GithubUser> query =
//            ds
//                .creteQuery(GithubUser.class)
//                .field("followers")
//                .equal(0);
//        ds.save(query, update);
    }

//    @Test(dependsOnMethods = {"repositories"}, expectedExceptions = {ConcurrentModificationException.class})
    public void versioned() {
//        final Organization org1 =
//            ds
//                .createQuery(Organization.class)
//                .get();
//        final Organization org2 =
//            ds
//                .createQuery(Organization.class)
//                .get();
//
//        assertEquals(org1.version, 1L);
//        ds.save(org1);
//
//        assertEquals(org1.version, 2L);
//        ds.save(org1);
//
//        assertEquals(org1.version, 3L);
//        ds.save(org2);
    }
}
