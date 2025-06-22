package com.virtualcompanion.moderationservice.controller;

public class RuleEngineController {

    private final RuleEngineService ruleEngineService;

    @PostMapping("/test")
    @Operation(summary = "Test moderation rules")
    public ResponseEntity<RuleTestResponse> testRules(
            @Valid @RequestBody RuleTestRequest request) {

        RuleTestResponse response = ruleEngineService.testRules(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reload")
    @Operation(summary = "Reload moderation rules")
    public ResponseEntity<Void> reloadRules() {
        ruleEngineService.reloadRules();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate rule configuration")
    public ResponseEntity<RuleValidationResponse> validateRules() {
        RuleValidationResponse response = ruleEngineService.validateRules();
        return ResponseEntity.ok(response);
    }
}
