package com.github.eseoa.searchEngine.services;

import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;
import com.github.eseoa.searchEngine.exceptions.IndexingIsNotRunningException;
import com.github.eseoa.searchEngine.exceptions.IndexingIsRunningException;
import com.github.eseoa.searchEngine.exceptions.SiteNotFoundException;
import com.github.eseoa.searchEngine.parser.IndexingStarter;
import com.github.eseoa.searchEngine.parser.PageParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class IndexingService {

    private static final int THREADS_COUNT = 5;

    private static ExecutorService executorService;

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    @Value("#{${sites}}")
    private TreeMap<String, String> urlTitle;

    @Autowired
    public IndexingService(SiteRepository siteRepository,
                           LemmaRepository lemmaRepository,
                           PageRepository pageRepository,
                           IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
    }

    public boolean startFullIndexing () {
        if (!nowIndexing()) {
            deleteAll();
            executorService = Executors.newFixedThreadPool(THREADS_COUNT);
            for (Map.Entry<String, String> entry : urlTitle.entrySet()) {
                String url = entry.getKey();
                String siteName = entry.getValue();
                executorService.submit(
                        new IndexingStarter( url, siteName, siteRepository, lemmaRepository )
                );
            }
            return true;
        }
        throw new IndexingIsRunningException();
    }

    public boolean stopIndexing() {
        if (nowIndexing() && ( executorService != null) && !executorService.isShutdown() && !executorService.isTerminated()) {
            executorService.shutdownNow();
            return true;
        }
        if (nowIndexing()) {
            ArrayList<Site> sites = (ArrayList<Site>) siteRepository.findAllByStatus(SiteStatus.INDEXING);
            sites.forEach(site -> siteRepository.setStatusById(SiteStatus.INDEXED, site.getId()));
            return true;
        }
        throw new IndexingIsNotRunningException();
    }

    public boolean indexPage(String url) {
        if (urlTitle.keySet().stream().anyMatch(url::contains)) {
            String startUrl = urlTitle.keySet().stream().filter(url::contains).findFirst().get();
            Optional<Site> mainSite = siteRepository.findByUrl(startUrl);
            new Thread(() -> {
                if (mainSite.isPresent()) {
                    mainExistIndexPage(startUrl, url);
                } else {
                    mainNotExistIndexPage(url);
                }
            }).start();
            return true;
        }
        throw new SiteNotFoundException();
    }

    private void mainExistIndexPage(String startUrl, String url) {
        boolean isIndexing = false;
        Site site = siteRepository.findByUrl(startUrl).get();
        if (site.getStatus().equals(SiteStatus.FAILED) || site.getStatus().equals(SiteStatus.INDEXED)) {
            isIndexing = true;
            siteRepository.setTimeAndStatusById(LocalDateTime.now(), SiteStatus.INDEXING, site.getId());
        }
        new PageParser(site.getId(), url).parse();
        if (siteRepository.getById(site.getId()).getStatus().equals(SiteStatus.INDEXING) && isIndexing) {
            siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
        }
        siteRepository.setTimeById(LocalDateTime.now(), site.getId());
    }

    private void mainNotExistIndexPage(String url)  {
        String siteUrl = urlTitle.keySet().stream().filter(url::contains).findFirst().get();
        String siteName = urlTitle.get(siteUrl);
        Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteUrl, siteName);
        siteRepository.save(site);
        new PageParser(site.getId(), url).parse();
        Optional<Site> mainSite = siteRepository.findById(site.getId());
        if (mainSite.get().getStatus().equals(SiteStatus.INDEXING)) {
            siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
        }
        siteRepository.setTimeById(LocalDateTime.now(), site.getId());
    }

    private boolean nowIndexing() {
        if (siteRepository.findAllByStatus(SiteStatus.INDEXING).size() == 0) {
            return false;
        }
        return true;
    }

    private void deleteAll(){
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();
    }
}
