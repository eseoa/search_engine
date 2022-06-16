package com.github.eseoa.searchEngine.responses.statistics;

import com.github.eseoa.searchEngine.main.entities.Site;
import com.github.eseoa.searchEngine.main.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.SiteRepository;
import lombok.Data;
import org.hibernate.Session;

import java.util.ArrayList;

@Data
public class Statistics {
    Total total;
    ArrayList<Detailed> detailed = new ArrayList<>();

    public Statistics(LemmaRepository lemmaRepository, SiteRepository siteRepository, PageRepository pageRepository) {
        total = new Total(lemmaRepository, siteRepository, pageRepository);
        ArrayList<Site> sites = (ArrayList<Site>) siteRepository.findAll();
        for(Site site : sites) {
            detailed.add(new Detailed(lemmaRepository, pageRepository, site));
        }
    }
}
