package com.github.eseoa.searchEngine.responses.statistics;

import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;
import lombok.Data;

@Data
public class Total {
    long sites;
    long pages;
    long lemmas;
    boolean isIndexing;

    public Total(IndexRepository indexRepository, SiteRepository siteRepository, PageRepository pageRepository) {
        sites = siteRepository.count();
        pages = pageRepository.count();
        lemmas = indexRepository.count();
        isIndexing = true;
    }
}
