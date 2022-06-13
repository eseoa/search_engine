package com.github.eseoa.searchEngine.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private boolean result;
    private String error;
}
