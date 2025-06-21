package com.virtualcompanion.userservice.repository;

public interface UserMapper {
    
    @Mapping(target = "roles", expression = "java(user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))")
    UserResponse toResponse(User user);
}
