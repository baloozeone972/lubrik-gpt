package com.virtualcompanion.mediaservice.dto;

public class VoiceSettings {

    private Double speed; // 0.5 - 2.0
    private Double pitch; // -20 to 20
    private Double volume; // 0.0 - 1.0
    private String emotion; // neutral, happy, sad, angry, etc.
    private Double emotionIntensity; // 0.0 - 1.0
    private String language;
    private String style; // chat, narration, customerservice
}
