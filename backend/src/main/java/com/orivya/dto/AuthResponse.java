package com.orivya.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    public String token;
    public String tokenType;
    public Long userId;
    public String name;
    public String email;
    public String role;
    public String message;
}
