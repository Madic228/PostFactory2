package com.example.postfactory2.Generate;

public class NewsItem {
    private String title;
    private String publicationDate;
    private String source;
    private String link;
    private String summarizedText;

    public NewsItem(String title, String publicationDate, String source, String link, String summarizedText) {
        this.title = title;
        this.publicationDate = publicationDate;
        this.source = source;
        this.link = link;
        this.summarizedText = summarizedText;
    }

    public String getTitle() {
        return title;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public String getSource() {
        return source;
    }

    public String getLink() {
        return link;
    }

    public String getSummarizedText() {
        return summarizedText;
    }
}
