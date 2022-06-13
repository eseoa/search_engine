package com.github.eseoa.searchEngine.main.controllers;


import com.github.eseoa.searchEngine.HibernateUtil;
import com.github.eseoa.searchEngine.config.Configuration;
import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.parser.IndexingStarter;
import com.github.eseoa.searchEngine.parser.PageParser;
import com.github.eseoa.searchEngine.responses.*;
import com.github.eseoa.searchEngine.seacrh.Search;
import com.github.eseoa.searchEngine.seacrh.SearchResult;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequestMapping("")
@EnableConfigurationProperties(value = Configuration.class)
public class MainPageController {

    private static final int THREADS_COUNT = 5;
    private static ExecutorService executorService;
    private static final TreeMap<String, String> urlName = new TreeMap<>();

    @Autowired
    public MainPageController(Configuration configuration) {
        ArrayList<String> sites = new ArrayList<>(configuration.getSites().values());
        PageParser.userAgent = configuration.getUserAgent();
        for (int i = 0; i < sites.size(); i = i + 2) {
            urlName.put(sites.get(i), sites.get(i + 1));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity statistics() {
        try (Session session = HibernateUtil.getHibernateSession()) {
            StatisticsResponse statisticsResponse = new StatisticsResponse(session, true);
            return new ResponseEntity<>(statisticsResponse, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false, "НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        try (Session session = HibernateUtil.getHibernateSession()) {
            if (nowIndexing(session) && ( executorService != null) && !executorService.isShutdown() && !executorService.isTerminated()) {
                executorService.shutdownNow();
                return new ResponseEntity<>(new StopIndexingResponse(true), HttpStatus.OK);
            }
            if (nowIndexing(session)) {
                session.beginTransaction();
                ArrayList<Site> sites = (ArrayList<Site>) session
                        .createQuery("FROM Site WHERE status = 'INDEXING'").list();
                sites.forEach(site -> {
                    site.setStatus(SiteStatus.INDEXED);
                    session.update(site);
                });
                session.getTransaction().commit();
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
        try (Session session = HibernateUtil.getHibernateSession()) {
            session.beginTransaction();
            if (!nowIndexing(session)) {
                session.createQuery("delete from Index").executeUpdate();
                session.createQuery("delete from Lemma").executeUpdate();
                session.createQuery("delete from Page").executeUpdate();
                session.createQuery("delete from Site").executeUpdate();
                executorService = Executors.newFixedThreadPool(THREADS_COUNT);
                for (Map.Entry<String, String> entry : urlName.entrySet()) {
                    executorService.submit(new IndexingStarter(entry.getKey(), entry.getValue()));
                }
                session.getTransaction().commit();
                return new ResponseEntity<>(new StartIndexingResponse(true), HttpStatus.OK);
            }
            session.getTransaction().commit();
            return new ResponseEntity<>(new ErrorResponse(true, "индексация уже запущена"), HttpStatus.OK);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url) throws InterruptedException {
        try (Session session = HibernateUtil.getHibernateSession()) {
            session.beginTransaction();
            if (urlName.keySet().stream().anyMatch(url::contains)) {
                String startUrl = urlName.keySet().stream().filter(url::contains).findFirst().get();
                Optional<Site> mainSite = session.createQuery("FROM Site WHERE url = :url ")
                        .setParameter("url", startUrl)
                        .stream()
                        .findFirst();
                if (mainSite.isPresent()) {
                    mainExistIndexPage(mainSite, url, session);
                } else {
                    mainNotExistIndexPage(url, session);
                }
                session.getTransaction().commit();
                return new ResponseEntity<>(new IndexPageResponse(true), HttpStatus.OK);
            }
            session.getTransaction().commit();
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
                searchResults = Search.search(params.get("query"));
                ArrayList<SearchResult> searchResultsLimit = getSearchListLimit(searchResults, offset, limit);
                SearchResponse searchResponse = new SearchResponse(true, searchResults.size(), searchResultsLimit);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            }
            if(urlName.keySet().contains(params.get("site"))) {
                searchResults = Search.search(params.get("query"), params.get("site"));
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

    private boolean nowIndexing(Session session) {
        if (session.createQuery("FROM Site WHERE status = 'INDEXING'").list().size() == 0) {
            return false;
        }
        return true;
    }

    private void mainExistIndexPage(Optional<Site> mainSite, String url, Session session) {
        boolean isIndexing = false;
        Site site = mainSite.get();
        if (site.getStatus().equals(SiteStatus.FAILED) || site.getStatus().equals(SiteStatus.INDEXED)) {
            isIndexing = true;
            site.setStatus(SiteStatus.INDEXING);
            site.setDateTime(LocalDateTime.now());
            session.update(site);
//            session.getTransaction().commit();
        }
        new PageParser(session, url, site.getId()).parse();
        if (site.getStatus().equals(SiteStatus.INDEXING) && isIndexing) {
            site.setDateTime(LocalDateTime.now());
            site.setStatus(SiteStatus.INDEXED);
            session.update(site);
        }
//        session.getTransaction().commit();
    }

    private void mainNotExistIndexPage(String url, Session session) {
        String siteUrl = urlName.keySet().stream().filter(url::contains).findFirst().get();
        String siteName = urlName.get(siteUrl);
        Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteUrl, siteName);
        session.save(site);
//        session.getTransaction().commit();
        new PageParser(session, url, site.getId()).parse();
        Optional<Site> mainSite = session.createQuery("FROM Site WHERE id = :id ")
                .setParameter("id", site.getId())
                .stream()
                .findFirst();
        if (mainSite.get().getStatus().equals(SiteStatus.INDEXING)) {
            site.setStatus(SiteStatus.INDEXED);
            session = HibernateUtil.getHibernateSession();
            session.update(site);
//            session.getTransaction().commit();
        }
    }

}
