package com.github.eseoa.searchEngine.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class StartIndexingResponse implements ResponseMarker {
    private boolean result;
}