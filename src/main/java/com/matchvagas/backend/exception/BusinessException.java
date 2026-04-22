package com.matchvagas.backend.exception;

import com.matchvagas.backend.security.JwtTokenProvider;
import com.matchvagas.backend.service.CustomUserDetailsService;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
