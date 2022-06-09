package main.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResponseError implements SearchResponseMarker{
    private boolean result;
    private String error;
}
