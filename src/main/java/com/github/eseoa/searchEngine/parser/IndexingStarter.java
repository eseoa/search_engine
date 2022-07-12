package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;

public class IndexingStarter implements Runnable {


    private static final int WAIT_TIME = 5_000;
    private final String url;
    private final String name;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;


    public IndexingStarter(String url, String name, SiteRepository siteRepository, LemmaRepository lemmaRepository) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.url = url;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, url, name);
            siteRepository.save(site);
            LinksParser.setCANCEL(false);
            LinksParser linksParser = new LinksParser(url, url, site);
            forkJoinPool.submit(linksParser);
            while (true) {
                if (Thread.interrupted()) {
                    forkJoinPool.shutdownNow();
                    LinksParser.setCANCEL(true);
                    break;
                }
                if (forkJoinPool.isQuiescent()) {
                    break;
                }
            }
            Thread.sleep(WAIT_TIME);
            site = siteRepository.getById(site.getId());
            long lemmasCount =  lemmaRepository.countBySiteId(site.getId());
            if (lemmasCount == 0) {
                siteRepository.setStatusById(SiteStatus.FAILED, site.getId());;
            }
            if (!site.getStatus().equals(SiteStatus.FAILED)) {
                siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
            }
            site.setDateTime(LocalDateTime.now());
            siteRepository.setTimeById(LocalDateTime.now(), site.getId());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}