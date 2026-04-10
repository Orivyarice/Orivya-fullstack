package com.orivya.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Name is required") public String name;
    @Email @NotBlank(message = "Email is required") public String email;
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") public String password;
    public String phone;
    public String address;
}
