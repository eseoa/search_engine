package com.github.eseoa.searchEngine.responses.statistics;

import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;
import lombok.Data;

import java.util.ArrayList;

@Data
public class Statistics {
    Total total;
    ArrayList<Detailed> detailed = new ArrayList<>();

    public Statistics(LemmaRepository lemmaRepository, SiteRepository siteRepository, PageRepository pageRepository,
                      IndexRepository indexRepository) {
        total = new Total(indexRepository, siteRepository, pageRepository);
        ArrayList<Site> sites = (ArrayList<Site>) siteRepository.findAll();
        for(Site site : sites) {
            detailed.add(new Detailed(lemmaRepository, pageRepository, site));
        }
    }
}
