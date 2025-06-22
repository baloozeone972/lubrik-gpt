package com.virtualcompanion.moderationservice.entity;

public class ModerationRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(n = "rule_n", nullable = false)
    private String ruleN;
    
    @Column(n = "description")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(n = "rule_type", nullable = false)
    private RuleType ruleType;
    
    @Column(n = "pattern", columnDefinition = "TEXT")
    private String pattern; // Regex ou keywords
    
    @Column(n = "category")
    private String category;
    
    @Column(n = "severity")
    private Integer severity; // 1-10
    
    @Column(n = "action", nullable = false)
    private String action; // BLOCK, FLAG, WARN, PASS
    
    @Column(n = "is_active")
    private boolean isActive = true;
    
    @Column(n = "applies_to_content_types")
    private String appliesToContentTypes; // Comma-separated
    
    @Column(n = "jurisdiction")
    private String jurisdiction; // Pays/région spécifique
    
    public enum RuleType {
        KEYWORD,
        REGEX,
        AI_THRESHOLD,
        BLACKLIST,
        WHITELIST,
        CUSTOM
    }
}
