package com.github.eseoa.searchEngine.responses.statistics;

import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.PageRepository;
import lombok.Data;

@Data
public class Detailed {
    private String url;
    private String name;
    private String status;
    private String statusTime;
    private String error;
    private long pages;
    private long lemmas;


    public Detailed(LemmaRepository lemmaRepository, PageRepository pageRepository, Site site) {
        url = site.getUrl();
        name = site.getName();
        status = String.valueOf(site.getStatus());
        statusTime = String.valueOf(site.getDateTime());
        error = site.getLastError();
        pages = pageRepository.countBySiteId(site.getId());
        lemmas = lemmaRepository.countBySiteId(site.getId());


    }
}
