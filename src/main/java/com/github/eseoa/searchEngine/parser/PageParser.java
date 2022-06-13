package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.entities.Index;
import com.github.eseoa.searchEngine.entities.Lemma;
import com.github.eseoa.searchEngine.entities.Page;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.lemmitization.LemmasGenerator;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

@Component
public class PageParser {
    public static String userAgent;
    private static final float TITLE_RANK = 1.0F;
    private static final float BODY_RANK = 0.8F;
    private final Session session;
    private final String url;
//    private Site site;
    private final int siteId;
    private Document document;
    private Page page;

    public PageParser(Session session, String url, int siteId) {
        this.session = session;
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
                return null ;
            }
            Optional<Page> pageOptional = session.createQuery("FROM Page WHERE path = :path")
                    .setParameter("path", url)
                    .stream()
                    .findFirst();
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
            session.beginTransaction();
            Query query = session.createQuery("update Site set dateTime = :dateTime, lastError = :message where id = :id");
            query.setParameter("id", siteId);
            query.setParameter("dateTime", LocalDateTime.now());
            query.setParameter("message", document.connection().response().statusMessage());
            query.executeUpdate();
            session.save(page);
            session.getTransaction().commit();
//            session.getTransaction().commit();
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
        session.beginTransaction();
        session.save(page);
        session.getTransaction().commit();
        Query query = session.createQuery("FROM Lemma WHERE lemma = :lemma AND siteId = :siteId");
        for(String sLemma : pageLemmas.keySet()) {
            if(Thread.interrupted()) {
                session.beginTransaction();
                session.createQuery("update Site set dateTime = :dateTime, status = :status where id = :id")
                        .setParameter("id", siteId)
                        .setParameter("dateTime", LocalDateTime.now())
                        .setParameter("status", SiteStatus.INDEXED)
                        .executeUpdate();
                session.getTransaction().commit();
//                session.getTransaction().commit();
                return;
            }
            float rank;
            Lemma dBLemma = saveAndGetLemma(query, sLemma);
            rank = titleLemmas.getOrDefault(sLemma, 0) * TITLE_RANK;
            rank += bodyLemmas.getOrDefault(sLemma, 0) * BODY_RANK;
            session.beginTransaction();
            session.save(new Index(page, dBLemma, rank));
            session.getTransaction().commit();
//            session.getTransaction().commit();
        }
        updateSiteTime();
//        session.getTransaction().commit();
    }

    private Lemma saveAndGetLemma (Query query, String sLemma) {
        Lemma dBLemma;
        Optional<Lemma> optional = query
                .setParameter("lemma", sLemma)
                .setParameter("siteId", siteId)
                .stream()
                .findFirst();
        if(optional.isEmpty()) {
            session.beginTransaction();
            dBLemma = new Lemma(sLemma, 1, siteId);
            session.save(dBLemma);
            session.getTransaction().commit();
        }
        else {
            session.beginTransaction();
            dBLemma = optional.get();
            dBLemma.setFrequency(dBLemma.getFrequency() + 1);
            session.update(dBLemma);
            session.getTransaction().commit();
        }
//        session.getTransaction().commit();
        return dBLemma;

    }

    private void forException (String message) {
        long lemmasCount = (long) session.createQuery("SELECT COUNT(*) FROM Lemma WHERE siteId = :siteId")
                .setParameter("siteId", siteId)
                .stream()
                .findFirst().get();
        session.beginTransaction();
        Query query = session.createQuery("update Site set dateTime = :dateTime, lastError = :message where id = :id");
        query.setParameter("id", siteId);
        query.setParameter("dateTime", LocalDateTime.now());
        query.setParameter("message", message);
        query.executeUpdate();
        if(lemmasCount == 0) {
            session.createQuery("update Site set status = :status where id = :id")
                    .setParameter("id", siteId)
                    .setParameter("status", SiteStatus.FAILED)
                    .executeUpdate();
        }
        session.getTransaction().commit();
//        session.getTransaction().commit();
    }

    private void clearDB (Page page) {
        for(Lemma lemma : page.getLemmas()) {
            int newFrequency = lemma.getFrequency() - 1;
            if(newFrequency == 0) {
                session.createQuery("DELETE FROM Lemma WHERE id = :id").setParameter("id", lemma.getId()).executeUpdate();
            }
            else {
                session.beginTransaction();
                lemma.setFrequency(newFrequency);
                session.update(lemma);
                session.getTransaction().commit();
            }
        }
        session.beginTransaction();
        session.createQuery("DELETE FROM Page WHERE id = :id").setParameter("id", page.getId()).executeUpdate();
        session.getTransaction().commit();
    }

    private void checkLemmaMap (HashMap<String, Integer> pageLemmas, String message) {
        if(pageLemmas.isEmpty()) {
            session.beginTransaction();
            Query query = session.createQuery("update Site set dateTime = :dateTime, lastError = :message where id = :id");
            query.setParameter("dateTime", LocalDateTime.now());
            query.setParameter("message", message);
            query.setParameter("id", siteId);
            query.executeUpdate();
            session.getTransaction().commit();
        }
    }

    private void updateSiteTime () {
        session.beginTransaction();
        Query query = session.createQuery("update Site set dateTime = :dateTime where id = :id");
        query.setParameter("id", siteId);
        query.setParameter("dateTime", LocalDateTime.now());
        query.executeUpdate();
        session.getTransaction().commit();

    }


}
