package com.orivya.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
}
