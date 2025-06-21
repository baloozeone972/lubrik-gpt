package com.virtualcompanion.conversationservice.document;

public class MessageAttachment {
    private String type; // image, audio, file
    private String url;
    private String mimeType;
    private Long size;
    private String n;
    private Map<String, Object> metadata;
}
