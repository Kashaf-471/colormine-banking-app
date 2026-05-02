package com.colormine.banking.models;

public class Contact {
    private String id;
    private String name;
    private String username;
    private String avatarInitial;

    public Contact(String id, String name, String username, String avatarInitial) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.avatarInitial = avatarInitial;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getAvatarInitial() { return avatarInitial; }
}
