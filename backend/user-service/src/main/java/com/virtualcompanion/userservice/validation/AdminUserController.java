package com.virtualcompanion.userservice.validation;

public class AdminUserController {

    private final UserService userService;
    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Get all users (paginated)")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        PageResponse<UserResponse> users = adminService.getUsers(pageable, search, status);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID userId) {
        UserResponse user = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable UUID userId,
            @RequestParam String reason) {

        adminService.suspendUser(userId, reason);
        return ResponseEntity.ok(ApiResponse.success("User suspended", null));
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable UUID userId) {
        adminService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User activated", null));
    }

    @PostMapping("/{userId}/verify-age")
    @Operation(summary = "Manually verify user age")
    public ResponseEntity<ApiResponse<Void>> verifyUserAge(
            @PathVariable UUID userId,
            @RequestParam String method) {

        adminService.verifyUserAge(userId, method);
        return ResponseEntity.ok(ApiResponse.success("Age verified", null));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user (Super Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId,
            @RequestParam boolean immediate) {

        adminService.deleteUser(userId, immediate);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}
