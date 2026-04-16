package com.matchvagas.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String message;
    private String error;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, String> erros;


    public ErrorResponse(int status, String error, String message, LocalDateTime now) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = now;
    }
}
