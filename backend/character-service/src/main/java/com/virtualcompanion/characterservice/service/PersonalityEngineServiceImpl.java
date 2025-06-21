package com.virtualcompanion.characterservice.service;

public class PersonalityEngineServiceImpl implements PersonalityEngineService {
    
    private final CharacterRepository characterRepository;
    
    @Override
    public Character generateCharacter(GenerateCharacterRequest request) {
        log.info("Generating character from prompt: {}", request.getPrompt());
        
        // This is a simplified implementation
        // In production, this would use a more sophisticated AI model
        
        Character character = Character.builder()
                .name(generateName(request.getPrompt()))
                .description(generateDescription(request.getPrompt()))
                .category(request.getCategory() != null ? request.getCategory() : "CUSTOM")
                .gender(parseGender(request.getGender()))
                .age(generateAge(request.getPrompt()))
                .backstory(generateBackstory(request.getPrompt()))
                .isPublic(false)
                .isNsfw(false)
                .build();
        
        return character;
    }
    
    @Override
    public CharacterPersonality generatePersonality(GenerateCharacterRequest request) {
        // Generate personality traits based on the prompt
        // This would use ML models in production
        
        Random random = new Random();
        
        CharacterPersonality personality = CharacterPersonality.builder()
                .openness(request.getPreferredTraits() != null && request.getPreferredTraits().getOpenness() != null ?
                        request.getPreferredTraits().getOpenness() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .conscientiousness(request.getPreferredTraits() != null && request.getPreferredTraits().getConscientiousness() != null ?
                        request.getPreferredTraits().getConscientiousness() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .extraversion(request.getPreferredTraits() != null && request.getPreferredTraits().getExtraversion() != null ?
                        request.getPreferredTraits().getExtraversion() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .agreeableness(request.getPreferredTraits() != null && request.getPreferredTraits().getAgreeableness() != null ?
                        request.getPreferredTraits().getAgreeableness() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .neuroticism(request.getPreferredTraits() != null && request.getPreferredTraits().getNeuroticism() != null ?
                        request.getPreferredTraits().getNeuroticism() : 0.5 + (random.nextDouble() - 0.5) * 0.4)
                .dominantTrait(determineDominantTrait(personality))
                .behaviorNotes(generateBehaviorNotes(request.getPrompt()))
                .build();
        
        return personality;
    }
    
    @Override
    public List<Character> getRecommendations(UUID userId, List<UserCharacter> userHistory, int limit) {
        // Analyze user's interaction history to find patterns
        Set<String> preferredCategories = new HashSet<>();
        Map<String, Integer> tagFrequency = new HashMap<>();
        
        for (UserCharacter uc : userHistory) {
            characterRepository.findById(uc.getCharacterId()).ifPresent(character -> {
                preferredCategories.add(character.getCategory());
                character.getTags().forEach(tag -> 
                    tagFrequency.merge(tag.getName(), 1, Integer::sum)
                );
            });
        }
        
        // Find similar characters
        List<Character> recommendations = characterRepository.findAll().stream()
                .filter(c -> c.getIsActive() && c.getIsPublic())
                .filter(c -> !userHistory.stream().anyMatch(uc -> uc.getCharacterId().equals(c.getId())))
                .sorted((c1, c2) -> {
                    int score1 = calculateRecommendationScore(c1, preferredCategories, tagFrequency);
                    int score2 = calculateRecommendationScore(c2, preferredCategories, tagFrequency);
                    return Integer.compare(score2, score1);
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        return recommendations;
    }
    
    @Override
    public double calculatePopularityScore(long conversations, Double rating, Long ratingCount, long uniqueUsers) {
        // Weighted formula for popularity
        double conversationScore = Math.log1p(conversations) * 0.3;
        double ratingScore = (rating != null ? rating : 0) * 0.4;
        double engagementScore = Math.log1p(uniqueUsers) * 0.2;
        double reviewScore = Math.log1p(ratingCount != null ? ratingCount : 0) * 0.1;
        
        return conversationScore + ratingScore + engagementScore + reviewScore;
    }
    
    private String generateName(String prompt) {
        // Simple name generation - in production would use NLG
        String[] nameTemplates = {
            "Luna", "Atlas", "Nova", "Orion", "Aria", "Phoenix", "Sage", "Echo"
        };
        return nameTemplates[new Random().nextInt(nameTemplates.length)];
    }
    
    private String generateDescription(String prompt) {
        // Generate a character description based on the prompt
        return "A unique character inspired by: " + prompt;
    }
    
    private Character.Gender parseGender(String gender) {
        if (gender == null) return Character.Gender.OTHER;
        try {
            return Character.Gender.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Character.Gender.OTHER;
        }
    }
    
    private int generateAge(String prompt) {
        // Generate appropriate age based on context
        return 25 + new Random().nextInt(20);
    }
    
    private String generateBackstory(String prompt) {
        // Generate backstory - simplified version
        return "Born from the imagination sparked by '" + prompt + "', this character has a unique story to tell...";
    }
    
    private String determineDominantTrait(CharacterPersonality personality) {
        Map<String, Double> traits = new HashMap<>();
        traits.put("Openness", personality.getOpenness());
        traits.put("Conscientiousness", personality.getConscientiousness());
        traits.put("Extraversion", personality.getExtraversion());
        traits.put("Agreeableness", personality.getAgreeableness());
        traits.put("Neuroticism", personality.getNeuroticism());
        
        return traits.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Balanced");
    }
    
    private String generateBehaviorNotes(String prompt) {
        return "Character behavior influenced by: " + prompt;
    }
    
    private int calculateRecommendationScore(Character character, Set<String> preferredCategories, Map<String, Integer> tagFrequency) {
        int score = 0;
        
        if (preferredCategories.contains(character.getCategory())) {
            score += 10;
        }
        
        for (var tag : character.getTags()) {
            score += tagFrequency.getOrDefault(tag.getName(), 0);
        }
        
        score += (int) (character.getPopularityScore() * 5);
        
        return score;
    }
}
