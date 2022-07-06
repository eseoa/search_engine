package com.github.eseoa.searchEngine.main.controllers;

import com.github.eseoa.searchEngine.main.entities.Site;
import com.github.eseoa.searchEngine.main.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.main.entities.repositories.*;
import com.github.eseoa.searchEngine.parser.IndexingStarter;
import com.github.eseoa.searchEngine.parser.PageParser;
import com.github.eseoa.searchEngine.responses.*;
import com.github.eseoa.searchEngine.seacrh.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequestMapping("")
public class MainPageController {
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private PageRepository pageRepository;
    private IndexRepository indexRepository;
    @Value("#{${sites}}")
    private TreeMap<String, String> urlTitle;
    private static final int THREADS_COUNT = 5;
    private static ExecutorService executorService;

    @Autowired
    public MainPageController(SiteRepository siteRepository,
                              LemmaRepository lemmaRepository,
                              PageRepository pageRepository,
                              IndexRepository indexRepository,
                              Environment environment) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        PageParser.userAgent = environment.getProperty("userAgent");
    }

    @GetMapping("/statistics")
    public ResponseEntity<ResponseMarker> statistics() {
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
    public ResponseEntity<ResponseMarker> stopIndexing() {
        try {
            if (nowIndexing() && ( executorService != null) && !executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.shutdownNow();
                return new ResponseEntity<>(new StopIndexingResponse(true), HttpStatus.OK);
            }
            if (nowIndexing()) {
                ArrayList<Site> sites = (ArrayList<Site>) siteRepository.findAllByStatus(SiteStatus.INDEXING);
                sites.forEach(site -> siteRepository.setStatusById(SiteStatus.INDEXED, site.getId()));
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
    public ResponseEntity<ResponseMarker> startIndexing() {
        try {
            if (!nowIndexing()) {
                deleteAll();
                executorService = Executors.newFixedThreadPool(THREADS_COUNT);
                for (Map.Entry<String, String> entry : urlTitle.entrySet()) {
                    String url = entry.getKey();
                    String siteName = entry.getValue();
                    executorService.submit(new IndexingStarter(
                            url,
                            siteName,
                            siteRepository,
                            lemmaRepository,
                            indexRepository,
                            pageRepository));
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
    public ResponseEntity<ResponseMarker> indexPage(@RequestParam String url) {
        try {
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
    public ResponseEntity<ResponseMarker> search(@RequestParam HashMap<String, String> params) {
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
            if(urlTitle.keySet().contains(params.get("site"))) {
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
        String siteUrl = urlTitle.keySet().stream().filter(url::contains).findFirst().get();
        String siteName = urlTitle.get(siteUrl);
        Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteUrl, siteName);
        siteRepository.save(site);
        new PageParser(siteRepository, lemmaRepository, indexRepository, pageRepository, site.getId(), url).parse();
        Optional<Site> mainSite = siteRepository.findById(site.getId());
        if (mainSite.get().getStatus().equals(SiteStatus.INDEXING)) {
            siteRepository.setStatusById(SiteStatus.INDEXED, site.getId());
        }
        siteRepository.setTimeById(LocalDateTime.now(), site.getId());
    }

    private void deleteAll(){
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();
    }

}
