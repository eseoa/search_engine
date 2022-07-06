package com.github.eseoa.searchEngine.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IndexPageResponse implements ResponseMarker{
    private boolean result;
}