package com.virtualcompanion.conversationservice.dto;

public enum MessageType {
    // Incoming
    MESSAGE,
    TYPING,
    READ_RECEIPT,
    JOIN_CONVERSATION,
    LEAVE_CONVERSATION,
    VOICE_START,
    VOICE_END,
    
    // Outgoing
    MESSAGE_SENT,
    AI_RESPONSE_CHUNK,
    USER_TYPING,
    MESSAGES_READ,
    CONVERSATION_HISTORY,
    VOICE_SESSION_STARTED,
    VOICE_SESSION_ENDED,
    ERROR
}
