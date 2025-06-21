package com.virtualcompanion.conversationservice;

public class ConversationContext {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private UUID conversationId;
    
    private CharacterContext characterContext;
    
    private UserContext userContext;
    
    private List<ContextMemory> shortTermMemory;
    
    private List<ContextMemory> longTermMemory;
    
    private Map<String, Object> sessionVariables;
    
    private String currentTopic;
    
    private List<String> topicHistory;
    
    private RelationshipState relationshipState;
    
    private LocalDateTime lastUpdated;
}
