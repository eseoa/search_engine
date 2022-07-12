package com.github.eseoa.searchEngine.controllers;

import com.github.eseoa.searchEngine.entities.repositories.*;
import com.github.eseoa.searchEngine.exceptions.*;
import com.github.eseoa.searchEngine.parser.PageParser;
import com.github.eseoa.searchEngine.responses.*;
import com.github.eseoa.searchEngine.seacrh.*;
import com.github.eseoa.searchEngine.services.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("")
public class MainPageController {
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SearchService searchService;
    private final IndexingService indexingService;

    @Autowired
    public MainPageController(SiteRepository siteRepository,
                              LemmaRepository lemmaRepository,
                              PageRepository pageRepository,
                              IndexRepository indexRepository,
                              SearchService searchService,
                              IndexingService indexingService,
                              Environment environment) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.searchService = searchService;
        this.indexingService = indexingService;
        PageParser.setSiteRepository(siteRepository);
        PageParser.setLemmaRepository(lemmaRepository);
        PageParser.setPageRepository(pageRepository);
        PageParser.setIndexRepository(indexRepository);
        PageParser.setUserAgent(environment.getProperty("userAgent"));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ResponseMarker> statistics() {
        try {
            StatisticsResponse statisticsResponse = new StatisticsResponse(lemmaRepository,
                    siteRepository,
                    pageRepository,
                    true,
                    indexRepository);
            return new ResponseEntity<>(statisticsResponse, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false, "НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<ResponseMarker> startIndexing() {
        try {
            indexingService.startFullIndexing();
            return new ResponseEntity<>(new StartIndexingResponse(true), HttpStatus.OK);
        }
        catch (IndexingIsRunningException e) {
            return new ResponseEntity<>(new ErrorResponse(true, "Индексация уже запущена"), HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false,"Неизвестная ошибка на сервере"), HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<ResponseMarker> stopIndexing() {
        try {
            indexingService.stopIndexing();
            return new ResponseEntity<>(new StopIndexingResponse(true), HttpStatus.OK);
        }
        catch (IndexingIsNotRunningException e) {
            return new ResponseEntity<>(new ErrorResponse(false, "Индексация не запущена"), HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false,"Неизвестная ошибка на сервере"), HttpStatus.OK);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<ResponseMarker> indexPage(@RequestParam String url) {
        try {
            indexingService.indexPage(url);
            return new ResponseEntity<>(new IndexPageResponse(true), HttpStatus.OK);
        }
        catch (SiteNotFoundException e) {
            ErrorResponse searchResponseError = new ErrorResponse(false, "Сайт находится за пределами " +
                    "файла конфигурации");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            ErrorResponse searchResponseError = new ErrorResponse(false, "Неизвестная ошибка на сервере");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseMarker> search(@RequestParam HashMap<String, String> params) {
        try {
            ArrayList<SearchResult> searchResults = searchService.search(params);
            int count = searchResults.size();
            ArrayList<SearchResult> searchResultsLimit = searchService.getSearchListLimit(searchResults, params);
            return new ResponseEntity<>(new SearchResponse(true, count, searchResultsLimit), HttpStatus.OK);
        }
        catch (EmptyRequestException e) {
            ErrorResponse searchResponseError = new ErrorResponse(false, "Задан пустой запрос");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
        catch (SiteNotFoundException e) {
            ErrorResponse searchResponseError = new ErrorResponse(false, "Сайт находится за пределами " +
                    "файла конфигурации");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            ErrorResponse searchResponseError = new ErrorResponse(false, "Неизвестная ошибка на сервере");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
    }

}
