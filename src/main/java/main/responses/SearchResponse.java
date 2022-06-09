package main.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import main.SearchResult;

import java.util.ArrayList;

@Data
@AllArgsConstructor
public class SearchResponse implements SearchResponseMarker {
    private boolean result;
    private long count;
    private ArrayList<SearchResult> data;


}
