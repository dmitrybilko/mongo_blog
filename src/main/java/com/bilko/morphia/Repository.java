package com.bilko.morphia;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Reference;

@Entity("repos")
public class Repository {

    @Id
    public String name;

    @Reference
    public Organization organization;

    @Reference
    public GithubUser owner;

    public Repository() { }

    public Repository(final Organization organization, final String name) {
        this.organization = organization;
        this.name = organization.name + "/" + name;
    }

    public Repository(final GithubUser owner, final String name) {
        this.owner = owner;
    }
}
