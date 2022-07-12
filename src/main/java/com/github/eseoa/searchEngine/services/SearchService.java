package com.github.eseoa.searchEngine.services;

import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;
import com.github.eseoa.searchEngine.exceptions.EmptyRequestException;
import com.github.eseoa.searchEngine.exceptions.SiteNotFoundException;
import com.github.eseoa.searchEngine.seacrh.Search;
import com.github.eseoa.searchEngine.seacrh.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SearchService {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Value("#{${sites}}")
    private TreeMap<String, String> urlTitle;

    @Autowired
    public SearchService(SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public ArrayList<SearchResult> search (Map<String, String> params) {
        ArrayList<SearchResult> searchResults;
        if(params.get("query") == null || params.get("query").trim().isEmpty()) {
            throw new EmptyRequestException();
        }
        if (params.get("site") == null) {
            searchResults = new Search(siteRepository, lemmaRepository, indexRepository).search(params.get("query"));
            return searchResults;
        }
        if(urlTitle.containsKey(params.get("site"))) {
            searchResults = new Search(siteRepository, lemmaRepository, indexRepository).search(params.get("query"), params.get("site"));
            return searchResults;
        }
        throw new SiteNotFoundException();
    }

    public ArrayList<SearchResult> getSearchListLimit (List<SearchResult> searchResults, Map<String, String> params) {
        int offset = Integer.parseInt(params.get("offset"));
        int limit = Integer.parseInt(params.get("limit"));
        ArrayList<SearchResult> searchResultsLimit = new ArrayList<>();
        int addend = Math.min(searchResults.size() - offset, limit);
        for (int i = offset ; i < offset + addend; i++) {
            searchResultsLimit.add(searchResults.get(i));
        }
        return searchResultsLimit;
    }
}
