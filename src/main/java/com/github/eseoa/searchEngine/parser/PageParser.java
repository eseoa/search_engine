package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.main.entities.Index;
import com.github.eseoa.searchEngine.main.entities.Lemma;
import com.github.eseoa.searchEngine.main.entities.Page;
import com.github.eseoa.searchEngine.main.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.lemmitization.LemmasGenerator;
import com.github.eseoa.searchEngine.main.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.main.entities.repositories.SiteRepository;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component
public class PageParser {
    public static String userAgent;
    private static final double TITLE_RANK = 1.0;
    private static final double BODY_RANK = 0.8;
    private final String url;
    private final int siteId;
    private Document document;
    private Page page;
    private SiteRepository siteRepository;
    private LemmaRepository lemmaRepository;
    private IndexRepository indexRepository;
    private PageRepository pageRepository;


    public PageParser(SiteRepository siteRepository,
                      LemmaRepository lemmaRepository,
                      IndexRepository indexRepository,
                      PageRepository pageRepository,
                      int siteId,
                      String url) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.url = url;
        this.siteId = siteId;
        try {
            this.document = Jsoup.connect(url).userAgent(userAgent).referrer("http://www.google.com").get();
        } catch (IOException e) {
            forException("Исключение при попытке присоедениться к странице: " + url);
        }
    }

    public Elements parse () {
        try {
            if(Thread.interrupted() || document == null) {
                return null;
            }
            Optional<Page> pageOptional = pageRepository.findByPath(url);
            if(pageOptional.isPresent()) {
                clearDB(pageOptional.get());
            }
            page = new Page(
                    document.connection().response().statusCode(),
                    url,
                    document.toString(),
                    siteId);
            if (isBadCode()) {
                return null;
            }
            workWithPage();
            return document.select("a");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(url);
            forException("Неизвестная ошибка на сервере");
            return null;
        }
    }

    private boolean isBadCode() {
        if(page.getCode() >= 400) {
            siteRepository.setTimeAndErrorById(LocalDateTime.now(),
                    document.connection().response().statusMessage(),
                    siteId);
            return true;
        }
        return false;

    }

    private void workWithPage () {
        HashMap<String, Integer> pageLemmas = LemmasGenerator.getLemmaCountMap(document.text());
        HashMap<String, Integer> titleLemmas = LemmasGenerator.getLemmaCountMap(document.title());
        HashMap<String, Integer> bodyLemmas = LemmasGenerator.getLemmaCountMap(document.body().text());
        checkLemmaMap(pageLemmas,page.getPath() + " пустой");
        checkLemmaMap(titleLemmas,page.getPath() + " заголовок пуст");
        checkLemmaMap(bodyLemmas,page.getPath() + " тело пустое");
        pageRepository.save(page);
        for(String sLemma : pageLemmas.keySet()) {
            if(Thread.interrupted()) {
                siteRepository.setTimeAndStatusById(LocalDateTime.now(), SiteStatus.INDEXED, siteId);

                return;
            }
            sLemma = sLemma.trim();
            double rank;
            Lemma dBLemma = saveAndGetLemma(sLemma);
            rank = titleLemmas.getOrDefault(sLemma, 0) * TITLE_RANK;
            rank += bodyLemmas.getOrDefault(sLemma, 0) * BODY_RANK;
            indexRepository.save(new Index(page, dBLemma, rank));
        }
        siteRepository.setTimeById(LocalDateTime.now(), siteId);
    }

    private Lemma saveAndGetLemma (String sLemma) {
        Lemma dBLemma;
        ArrayList<Lemma> lemmasList = (ArrayList<Lemma>) lemmaRepository.findBySiteIdAndLemma(siteId, sLemma);
        if(lemmasList.isEmpty()) {
            dBLemma = new Lemma(sLemma, 1, siteId);
            lemmaRepository.save(dBLemma);
            return dBLemma;
        }
        if(lemmasList.size() > 1) {
            lemmaRepository.deleteById(lemmasList.get(1).getId());
            lemmaRepository.setFrequencyById(lemmasList.get(0).getFrequency() + 2, lemmasList.get(0).getId());
        }
        else {
            lemmaRepository.setFrequencyById(lemmasList.get(0).getFrequency() + 1, lemmasList.get(0).getId());
        }
        dBLemma = lemmasList.get(0);
        return dBLemma;
    }

    private void forException (String message) {
        long lemmasCount = lemmaRepository.countBySiteId(siteId);
        siteRepository.setTimeAndErrorById(LocalDateTime.now(), message, siteId);
        if(lemmasCount == 0) {
            siteRepository.setStatusById(SiteStatus.FAILED, siteId);
        }

    }

    private void clearDB (Page page) {
        for(Lemma lemma : page.getLemmas()) {
            int newFrequency = lemma.getFrequency() - 1;
            if(newFrequency != 0) {
                lemmaRepository.setFrequencyById(newFrequency, lemma.getId());
            }
        }
        pageRepository.deleteById(page.getId());
    }

    private void checkLemmaMap (HashMap<String, Integer> pageLemmas, String message) {
        if(pageLemmas.isEmpty()) {
            siteRepository.setTimeAndErrorById(LocalDateTime.now(), message, siteId);
        }
    }

}
