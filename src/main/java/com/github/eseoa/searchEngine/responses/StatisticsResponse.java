package com.github.eseoa.searchEngine.responses;

import com.github.eseoa.searchEngine.main.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.SiteRepository;
import com.github.eseoa.searchEngine.responses.statistics.Statistics;
import lombok.Data;
import org.hibernate.Session;

@Data
public final class StatisticsResponse {
    private boolean result;
    private Statistics statistics;

    public StatisticsResponse(LemmaRepository lemmaRepository,
                              SiteRepository siteRepository,
                              PageRepository pageRepository,
                              Boolean result) {
        this.result = result;
        statistics = new Statistics(lemmaRepository, siteRepository, pageRepository);
    }

}