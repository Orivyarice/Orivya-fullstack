package com.orivya.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
}
