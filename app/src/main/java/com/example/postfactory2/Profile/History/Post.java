package com.example.postfactory2.Profile.History;

public class Post {
    private int id;
    private String title;
    private String content;
    private String date;
    private String status;
    private String socialNetworkUrl;

    public Post(String title, String content, String date, String status) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getExcerpt() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }
    
    public String getSocialNetworkUrl() {
        return socialNetworkUrl;
    }
    
    public void setSocialNetworkUrl(String socialNetworkUrl) {
        this.socialNetworkUrl = socialNetworkUrl;
    }
}
