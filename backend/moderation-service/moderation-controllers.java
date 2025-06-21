// ModerationController.java
package com.virtualcompanion.moderationservice.controller;

import com.virtualcompanion.moderationservice.dto.*;
import com.virtualcompanion.moderationservice.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
@Tag(name = "Moderation", description = "Content moderation endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ModerationController {
    
    private final ModerationService moderationService;
    
    @PostMapping("/text")
    @Operation(summary = "Moderate text content")
    public ResponseEntity<TextModerationResponse> moderateText(
            @Valid @RequestBody TextModerationRequest request) {
        
        TextModerationResponse response = moderationService.moderateText(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/image")
    @Operation(summary = "Moderate image content")
    public ResponseEntity<ImageModerationResponse> moderateImage(
            @Valid @RequestBody ImageModerationRequest request) {
        
        ImageModerationResponse response = moderationService.moderateImage(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/decisions/{moderationId}")
    @Operation(summary = "Get moderation decision")
    public ResponseEntity<ModerationDecision> getModerationDecision(
            @PathVariable UUID moderationId) {
        
        ModerationDecision decision = moderationService.getModerationDecision(moderationId);
        return ResponseEntity.ok(decision);
    }
    
    @PostMapping("/decisions/{moderationId}/review")
    @Operation(summary = "Review moderation decision")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> reviewModeration(
            @PathVariable UUID moderationId,
            @Valid @RequestBody ModerationDecision decision) {
        
        moderationService.reviewModeration(moderationId, decision);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/reports")
    @Operation(summary = "Report content")
    public ResponseEntity<ContentReportResponse> reportContent(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ContentReportRequest request) {
        
        request.setReporterId(userId);
        ContentReportResponse response = moderationService.reportContent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/appeals")
    @Operation(summary = "Submit appeal")
    public ResponseEntity<AppealResponse> submitAppeal(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AppealRequest request) {
        
        request.setUserId(userId);
        AppealResponse response = moderationService.submitAppeal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/age-verification")
    @Operation(summary = "Verify user age")
    public ResponseEntity<AgeVerificationResponse> verifyAge(
            @Valid @RequestBody AgeVerificationRequest request) {
        
        AgeVerificationResponse response = moderationService.verifyAge(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get moderation statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationStatistics> getStatistics(
            @RequestParam(defaultValue = "monthly") String period) {
        
        ModerationStatistics statistics = moderationService.getModerationStatistics(period);
        return ResponseEntity.ok(statistics);
    }
}

// AdminModerationController.java
package com.virtualcompanion.moderationservice.controller;

import com.virtualcompanion.moderationservice.dto.*;
import com.virtualcompanion.moderationservice.service.AdminModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
@Tag(name = "Admin Moderation", description = "Administrative moderation endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AdminModerationController {
    
    private final AdminModerationService adminService;
    
    @GetMapping("/queue")
    @Operation(summary = "Get moderation queue")
    public ResponseEntity<Page<ModerationQueueItem>> getModerationQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ModerationQueueItem> queue = adminService.getModerationQueue(status, priority, pageRequest);
        return ResponseEntity.ok(queue);
    }
    
    @GetMapping("/reports")
    @Operation(summary = "Get content reports")
    public ResponseEntity<Page<ContentReportDetail>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<ContentReportDetail> reports = adminService.getReports(status, pageRequest);
        return ResponseEntity.ok(reports);
    }
    
    @PostMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Resolve content report")
    public ResponseEntity<Void> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request) {
        
        adminService.resolveReport(reportId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/appeals")
    @Operation(summary = "Get appeals")
    public ResponseEntity<Page<AppealDetail>> getAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "submittedAt"));
        Page<AppealDetail> appeals = adminService.getAppeals(status, pageRequest);
        return ResponseEntity.ok(appeals);
    }
    
    @PostMapping("/appeals/{appealId}/review")
    @Operation(summary = "Review appeal")
    public ResponseEntity<Void> reviewAppeal(
            @PathVariable UUID appealId,
            @Valid @RequestBody ReviewAppealRequest request) {
        
        adminService.reviewAppeal(appealId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/users/{userId}/history")
    @Operation(summary = "Get user moderation history")
    public ResponseEntity<Page<UserModerationHistoryResponse>> getUserHistory(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserModerationHistoryResponse> history = adminService.getUserModerationHistory(userId, pageRequest);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/users/{userId}/actions")
    @Operation(summary = "Take action on user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> takeUserAction(
            @PathVariable UUID userId,
            @Valid @RequestBody UserActionRequest request) {
        
        adminService.takeUserAction(userId, request);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/rules")
    @Operation(summary = "Get moderation rules")
    public ResponseEntity<List<ModerationRuleResponse>> getRules(
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String jurisdiction) {
        
        List<ModerationRuleResponse> rules = adminService.getModerationRules(contentType, jurisdiction);
        return ResponseEntity.ok(rules);
    }
    
    @PostMapping("/rules")
    @Operation(summary = "Create moderation rule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationRuleResponse> createRule(
            @Valid @RequestBody CreateRuleRequest request) {
        
        ModerationRuleResponse rule = adminService.createRule(request);
        return ResponseEntity.ok(rule);
    }
    
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "Update moderation rule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModerationRuleResponse> updateRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateRuleRequest request) {
        
        ModerationRuleResponse rule = adminService.updateRule(ruleId, request);
        return ResponseEntity.ok(rule);
    }
    
    @DeleteMapping("/rules/{ruleId}")
    @Operation(summary = "Delete moderation rule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        adminService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/blocked-content")
    @Operation(summary = "Get blocked content")
    public ResponseEntity<Page<BlockedContentResponse>> getBlockedContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blockedAt"));
        Page<BlockedContentResponse> blocked = adminService.getBlockedContent(pageRequest);
        return ResponseEntity.ok(blocked);
    }
    
    @PostMapping("/blocked-content")
    @Operation(summary = "Block content")
    public ResponseEntity<Void> blockContent(
            @Valid @RequestBody BlockContentRequest request) {
        
        adminService.blockContent(request);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/blocked-content/{blockId}")
    @Operation(summary = "Unblock content")
    public ResponseEntity<Void> unblockContent(@PathVariable UUID blockId) {
        adminService.unblockContent(blockId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/reports/export")
    @Operation(summary = "Export moderation report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExportResponse> exportReport(
            @Valid @RequestBody ExportReportRequest request) {
        
        ExportResponse response = adminService.exportReport(request);
        return ResponseEntity.ok(response);
    }
}

// RuleEngineController.java
package com.virtualcompanion.moderationservice.controller;

import com.virtualcompanion.moderationservice.dto.RuleTestRequest;
import com.virtualcompanion.moderationservice.dto.RuleTestResponse;
import com.virtualcompanion.moderationservice.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/rule-engine")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Rule Engine", description = "Moderation rule engine endpoints")
@SecurityRequirement(name = "bearer-jwt")
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