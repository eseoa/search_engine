package com.github.eseoa.searchEngine.responses.statistics;

import com.github.eseoa.searchEngine.main.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.SiteRepository;
import lombok.Data;

@Data
public class Total {
    long sites;
    long pages;
    long lemmas;
    boolean isIndexing;

    public Total(LemmaRepository lemmaRepository, SiteRepository siteRepository, PageRepository pageRepository) {
        sites = siteRepository.count();
        pages = pageRepository.count();
        lemmas = lemmaRepository.count();
        isIndexing = true;
    }
}
