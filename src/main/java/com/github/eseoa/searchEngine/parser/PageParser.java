package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.entities.Index;
import com.github.eseoa.searchEngine.entities.Lemma;
import com.github.eseoa.searchEngine.entities.Page;
import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.PageRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.lemmitization.LemmasGenerator;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class PageParser {

    private static final double TITLE_RANK = 1.0;
    private static final double BODY_RANK = 0.8;

    @Setter
    private static SiteRepository siteRepository;
    @Setter
    private static LemmaRepository lemmaRepository;
    @Setter
    private static IndexRepository indexRepository;
    @Setter
    private static PageRepository pageRepository;
    @Setter
    private static String userAgent;

    private final String url;
    private final int siteId;
    private Document document;
    private Page page;

    private final ArrayList<Lemma> lemmasToSave = new ArrayList<>();
    private final ArrayList<Index> indexesToSave = new ArrayList<>();
    private static final Object lock = new Object();

    public PageParser(int siteId, String url) {
        this.url = url;
        this.siteId = siteId;
        try {
            this.document = Jsoup.connect(url).userAgent(userAgent).referrer("http://www.google.com").get();
        } catch (IOException e) {
            this.document = null;
            forException("Не подключиться к странице: " + url);
        }
    }

    public Elements parse () {
        try {
            if(document == null ) {
                return null;
            }
            if(Thread.interrupted()) {
                return null;
            }
            Optional<Page> pageOptional = pageRepository.findByPath(url);
            pageOptional.ifPresent(this::clearDB);
            page = new Page(
                    document.connection().response().statusCode(),
                    url,
                    document.toString(),
                    siteId);
            workWithPageLemmas();
            synchronized (lock) {
                if(Thread.interrupted()) {
                    return null;
                }
                pageRepository.save(page);
                lemmaRepository.saveAll(lemmasToSave);
                siteRepository.setTimeById(LocalDateTime.now(), siteId);
                indexRepository.saveAll(indexesToSave);
            }
            return document.select("a");
        } catch (Exception e) {
            e.printStackTrace();
            forException("неизвестная ошибка на сервере");
            return null;
        }
    }

    private void workWithPageLemmas() {
        HashMap<String, Integer> pageLemmas = LemmasGenerator.getLemmaCountMap(document.text());
        HashMap<String, Integer> titleLemmas = LemmasGenerator.getLemmaCountMap(document.title());
        HashMap<String, Integer> bodyLemmas = LemmasGenerator.getLemmaCountMap(document.body().text());
        checkLemmaMap(pageLemmas,page.getPath() + " страница пуста");
        checkLemmaMap(titleLemmas,page.getPath() + " заголовок пуст");
        checkLemmaMap(bodyLemmas,page.getPath() + " тело пустое");
        for(String sLemma : pageLemmas.keySet()) {
            sLemma = sLemma.trim();
            double rank;
            Lemma dBLemma = saveAndGetLemma(sLemma);
            rank = titleLemmas.getOrDefault(sLemma, 0) * TITLE_RANK;
            rank += bodyLemmas.getOrDefault(sLemma, 0) * BODY_RANK;
            indexesToSave.add(new Index(page, dBLemma, rank));
        }
    }

    private Lemma saveAndGetLemma (String sLemma) {
        lemmasToSave.add(new Lemma(sLemma, 1, siteId));
        return lemmasToSave.get(lemmasToSave.size() - 1);
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
