package main.controllers;

import main.*;
import main.entities.Site;
import main.entities.enums.SiteStatus;
import main.responses.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(value = Configuration.class)
public class MainPageController {

    private static final int THREADS_COUNT = 5;
    private static ExecutorService executorService;
    private static final TreeMap <String, String> urlName = new TreeMap<>();

    @Autowired
    public MainPageController (Configuration configuration) {
        ArrayList<String> sites = new ArrayList<>(configuration.getSites().values());
        PageParser.userAgent = configuration.getUserAgent();
        for(int i = 0; i < sites.size(); i = i + 2) {
            urlName.put(sites.get(i), sites.get(i+1));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity statistics(){
        try {
            Session session = HibernateUtil.getHibernateSession();
            StatisticsResponse statisticsResponse = new StatisticsResponse(session, true);
            return new ResponseEntity<>(statisticsResponse, HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        try {
            HashMap<String, String> response = new HashMap<>();
            if (nowIndexing()) {
                executorService.shutdownNow();
                return new ResponseEntity(new StopIndexingResponse(true), HttpStatus.OK);
            }
            return new ResponseEntity(new ErrorResponse(false, "индексация не запущена"), HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        try {
            if (!nowIndexing()) {
                Session session = HibernateUtil.getHibernateSession();
                Transaction transaction = session.beginTransaction();
                session.createQuery("delete from Index").executeUpdate();
                session.createQuery("delete from Lemma").executeUpdate();
                session.createQuery("delete from Page").executeUpdate();
                session.createQuery("delete from Site").executeUpdate();
                transaction.commit();
                session.close();
                executorService = Executors.newFixedThreadPool(THREADS_COUNT);
                for (Map.Entry<String, String> entry : urlName.entrySet()) {
                    executorService.submit(new IndexingStarter(entry.getKey(), entry.getValue()));
                }
                return new ResponseEntity(new StartIndexingResponse(true), HttpStatus.OK);
            }
            return new ResponseEntity(new ErrorResponse(false, "индексация уже запущена"), HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity mainExistIndexPage(@RequestParam String url) throws InterruptedException {
        try {
            if (urlName.keySet().stream().anyMatch(url::contains)) {
                String startUrl = urlName.keySet().stream().filter(url::contains).findFirst().get();
                Session session = HibernateUtil.getHibernateSession();
                Optional<Site> mainSite = session.createQuery("FROM Site WHERE url = :url ")
                        .setParameter("url", startUrl)
                        .stream()
                        .findFirst();
                session.close();
                if (mainSite.isPresent()) {
                    mainExistIndexPage(mainSite, url);
                } else {
                    mainNotExistIndexPage(url);
                }
                return new ResponseEntity<>(new IndexPageResponse(true), HttpStatus.OK);
            }
            return new ResponseEntity(
                    new ErrorResponse(false, "Данная страница находится за пределами сайтов, " +
                            "указанных в конфигурационном файле"), HttpStatus.OK);
        }
        catch (Exception e){
            return new ResponseEntity<>(new ErrorResponse(false,"НА СЕРВЕРЕ ПРОИЗОШЛА ОШИБКА"), HttpStatus.OK);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponseMarker> search(@RequestParam HashMap<String, String> params) {
        try {
            ArrayList<SearchResult> searchResults;
            int offset = Integer.parseInt(params.get("offset"));
            int limit = Integer.parseInt(params.get("limit"));
            if(params.get("query") == null || params.get("query").trim().isEmpty()) {
                SearchResponseError searchResponseError = new SearchResponseError(false, "задан пустой запрос");
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
            SearchResponseError searchResponseError = new SearchResponseError(false, "Сайт не найден");
            return new ResponseEntity<>(searchResponseError, HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            SearchResponseError searchResponseError = new SearchResponseError(false, "Неизвестная ошибка");
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
        Session session = HibernateUtil.getHibernateSession();
        if (session.createQuery("FROM Site WHERE status = 'INDEXING'").list().size() == 0) {
            session.close();
            return false;
        }
        session.close();
        return true;
    }

    private void mainExistIndexPage(Optional<Site> mainSite, String url) {
        boolean isIndexing = false;
        Site site = mainSite.get();
        Session session;
        if(site.getStatus().equals(SiteStatus.FAILED) || site.getStatus().equals(SiteStatus.INDEXED)) {
            isIndexing = true;
            session = HibernateUtil.getHibernateSession();
            Transaction transaction = session.beginTransaction();
            site.setStatus(SiteStatus.INDEXING);
            site.setDateTime(LocalDateTime.now());
            session.update(site);
            transaction.commit();
            session.close();
        }
        PageParser.parse(url, site);
        session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        if(site.getStatus().equals(SiteStatus.INDEXING) && isIndexing){
            site.setDateTime(LocalDateTime.now());
            site.setStatus(SiteStatus.INDEXED);
            session.update(site);
        }
        transaction.commit();
        session.close();
    }

    private void mainNotExistIndexPage (String url) {
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        String siteUrl = urlName.keySet().stream().filter(url::contains).findFirst().get();
        String siteName = urlName.get(siteUrl);
        Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, siteUrl, siteName);
        session.save(site);
        transaction.commit();
        session.close();
        PageParser.parse(url, site);
        if (site.getStatus().equals(SiteStatus.INDEXING)) {
            site.setStatus(SiteStatus.INDEXED);
            session = HibernateUtil.getHibernateSession();
            transaction = session.beginTransaction();
            session.update(site);
            transaction.commit();
            session.close();
        }
    }
}
