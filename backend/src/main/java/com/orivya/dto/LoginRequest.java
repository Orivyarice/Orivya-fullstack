package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class LoginRequest {
    @Email @NotBlank(message = "Email is required") public String email;
    @NotBlank(message = "Password is required") public String password;
}
