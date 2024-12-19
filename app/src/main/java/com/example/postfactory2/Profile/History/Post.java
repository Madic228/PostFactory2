package com.example.postfactory2.Profile.History;

public class Post {
    private final String title;
    private final String excerpt;
    private final String date;
    private final String status;

    public Post(String title, String excerpt, String date, String status) {
        this.title = title;
        this.excerpt = excerpt;
        this.date = date;
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }
}
