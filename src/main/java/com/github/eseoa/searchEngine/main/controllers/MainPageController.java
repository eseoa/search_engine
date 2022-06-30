package com.github.eseoa.searchEngine.main.controllers;


import com.github.eseoa.searchEngine.config.Configuration;
import com.github.eseoa.searchEngine.main.entities.Site;
import com.github.eseoa.searchEngine.main.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.main.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.SiteRepository;
import com.github.eseoa.searchEngine.parser.IndexingStarter;
import com.github.eseoa.searchEngine.parser.PageParser;
import com.github.eseoa.searchEngine.responses.*;
import com.github.eseoa.searchEngine.seacrh.Search;
import com.github.eseoa.searchEngine.seacrh.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequestMapping("")
@EnableConfigurationProperties(value = Configuration.class)
public class MainPageController {
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;

    private static final int THREADS_COUNT = 5;
    private static ExecutorService executorService;
    private static final TreeMap<String, String> urlName = new TreeMap<>();

    @Autowired
    public MainPageController(SiteRepository siteRepository,
                              LemmaRepository lemmaRepository,
                              PageRepository pageRepository,
                              IndexRepository indexRepository,
                              Configuration configuration) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        ArrayList<String> sites = new ArrayList<>(configuration.getSites().values());
        PageParser.userAgent = configuration.getUserAgent();
        for (int i = 0; i < sites.size(); i = i + 2) {
            urlName.put(sites.get(i), sites.get(i + 1));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity statistics() {
        try {
            StatisticsResponse statisticsResponse = new StatisticsResponse(lemmaRepository,
                    siteRepository,
                    pageRepository,
                    true);
            return new ResponseEntity<>(statisticsResponse, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false, "НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        try {
            if (nowIndexing() && ( executorService != null) && !executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.shutdownNow();
                return new ResponseEntity<>(new StopIndexingResponse(true), HttpStatus.OK);
            }
            if (nowIndexing()) {
                ArrayList<Site> sites = (ArrayList<Site>) siteRepository.findAllByStatus(SiteStatus.INDEXING);
                sites.forEach(site -> {
                    siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
                });
                return new ResponseEntity<>(new StopIndexingResponse(true), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ErrorResponse(false, "индексация не запущена"), HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        try {
            if (!nowIndexing()) {
                siteRepository.deleteAll();
                pageRepository.deleteAll();
                lemmaRepository.deleteAll();
                indexRepository.deleteAll();
                executorService = Executors.newFixedThreadPool(THREADS_COUNT);
                for (Map.Entry<String, String> entry : urlName.entrySet()) {
                    executorService.submit(new IndexingStarter(entry.getKey(), entry.getValue(),
                            siteRepository, lemmaRepository, indexRepository, pageRepository));
                }
                return new ResponseEntity<>(new StartIndexingResponse(true), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ErrorResponse(true, "индексация уже запущена"), HttpStatus.OK);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url) throws InterruptedException {
        try {
            if (urlName.keySet().stream().anyMatch(url::contains)) {
                String startUrl = urlName.keySet().stream().filter(url::contains).findFirst().get();
                Optional<Site> mainSite = siteRepository.findByUrl(startUrl);
                new Thread(() -> {
                    if (mainSite.isPresent()) {
                        mainExistIndexPage(startUrl, url);
                    } else {
                        mainNotExistIndexPage(url);
                    }
                }).start();
                return new ResponseEntity<>(new IndexPageResponse(true), HttpStatus.OK);
            }
            return new ResponseEntity<>(
                    new ErrorResponse(false, "Данная страница находится за пределами сайтов, " +
                            "указанных в конфигурационном файле"), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false, "НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/search")
    public ResponseEntity search(@RequestParam HashMap<String, String> params) {
        try {
            ArrayList<SearchResult> searchResults;
            int offset = Integer.parseInt(params.get("offset"));
            int limit = Integer.parseInt(params.get("limit"));
            if(params.get("query") == null || params.get("query").trim().isEmpty()) {
                ErrorResponse searchResponseError = new ErrorResponse(false, "задан пустой запрос");
                return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
            }
            if (params.get("site") == null) {
                searchResults = new Search(siteRepository, lemmaRepository, indexRepository).search(params.get("query"));
                ArrayList<SearchResult> searchResultsLimit = getSearchListLimit(searchResults, offset, limit);
                SearchResponse searchResponse = new SearchResponse(true, searchResults.size(), searchResultsLimit);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            }
            if(urlName.keySet().contains(params.get("site"))) {
                searchResults = new Search(siteRepository, lemmaRepository, indexRepository).search(params.get("query"), params.get("site"));
                ArrayList<SearchResult> searchResultsLimit = getSearchListLimit(searchResults, offset, limit);
                SearchResponse searchResponse = new SearchResponse(true, searchResults.size(), searchResultsLimit);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            }
            ErrorResponse searchResponseError = new ErrorResponse(false, "Сайт не найден");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            ErrorResponse searchResponseError = new ErrorResponse(false, "Неизвестная ошибка");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }

    }

    private ArrayList<SearchResult> getSearchListLimit (List<SearchResult> searchResults, int offset, int limit) {
        ArrayList<SearchResult> searchResultsLimit = new ArrayList<>();
        int addend = Math.min(searchResults.size() - offset, limit);
        for(int i = offset ; i < offset + addend; i++) {
            searchResultsLimit.add(searchResults.get(i));
        }
        return searchResultsLimit;
    }

    private boolean nowIndexing() {
        if (siteRepository.findAllByStatus(SiteStatus.INDEXING).size() == 0) {
            return false;
        }
        return true;
    }

    private void mainExistIndexPage(String startUrl, String url) {
        boolean isIndexing = false;
        Site site = siteRepository.findByUrl(startUrl).get();
        if (site.getStatus().equals(SiteStatus.FAILED) || site.getStatus().equals(SiteStatus.INDEXED)) {
            isIndexing = true;
            siteRepository.setTimeAndStatusById(LocalDateTime.now(), SiteStatus.INDEXING, site.getId());
        }

        new PageParser(siteRepository, lemmaRepository, indexRepository, pageRepository, site.getId(), url).parse();
        if (siteRepository.getById(site.getId()).getStatus().equals(SiteStatus.INDEXING) && isIndexing) {
            siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
        }
        siteRepository.setTimeById(LocalDateTime.now(), site.getId());
    }

    private void mainNotExistIndexPage(String url)  {
        String siteUrl = urlName.keySet().stream().filter(url::contains).findFirst().get();
        String siteName = urlName.get(siteUrl);
        Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteUrl, siteName);
        siteRepository.save(site);
        new PageParser(siteRepository, lemmaRepository, indexRepository, pageRepository, site.getId(), url).parse();
        Optional<Site> mainSite = siteRepository.findById(site.getId());
        if (mainSite.get().getStatus().equals(SiteStatus.INDEXING)) {
            siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
        }
        siteRepository.setTimeById(LocalDateTime.now(), site.getId());
    }

}
