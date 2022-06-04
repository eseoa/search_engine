package main;

import main.entities.Index;
import main.entities.Lemma;
import main.entities.Page;
import main.entities.Site;
import main.entities.enums.SiteStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
@Component
public class PageParser {

    private static final float TITLE_RANK = 1.0F;
    private static final float BODY_RANK = 0.8F;
    public static String userAgent;

    public static Elements parse (String url, Site site) {
        try {
            if(Thread.interrupted()){
                return null ;
            }
            Document doc = Jsoup.connect(url).userAgent(userAgent).referrer("http://www.google.com").get();
            Session session = HibernateUtil.getHibernateSession();
            Optional<Page> pageOptional = session.createQuery("FROM Page WHERE path = :path").setParameter("path", url).stream().findFirst();
            if(pageOptional.isPresent()) {
                clearDB(session, pageOptional.get());
            }
            session.close();
            Page page = new Page(
                    doc.connection().response().statusCode(),
                    url,
                    doc.toString(),
                    site.getId());
            if (ifPageBadCode(page, site, doc)) {
                return null;
            }
            workWithPage(page, site, doc);
            updateSiteTime(site);
            return doc.select("a");
        }
        catch (ConnectException e) {
            e.printStackTrace();
            System.out.println(url);
            forException("Страница недоступна", site);
            return null;
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println(url);
            forException("Input output exception " + e.getMessage(), site);
            return  null;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println(url);
            forException("Неизвестная ошибка, " + e.getMessage(), site);
            return null;
        }
    }

    private static boolean ifPageBadCode (Page page, Site site, Document doc) {
        if(page.getCode() >= 400) {
            Session session = HibernateUtil.getHibernateSession();
            Transaction transaction = session.beginTransaction();
            site.setLastError(doc.connection().response().statusMessage());
            site.setDateTime(LocalDateTime.now());
            session.update(site);
            session.save(page);
            transaction.commit();
            session.close();
            return true;
        }
        return false;

    }

    private static void workWithPage (Page page, Site site, Document doc) {
        HashMap<String, Integer> pageLemmas = LemmasGenerator.getLemmaCountMap(doc.text());
        HashMap<String, Integer> titleLemmas = LemmasGenerator.getLemmaCountMap(doc.title());
        HashMap<String, Integer> bodyLemmas = LemmasGenerator.getLemmaCountMap(doc.body().text());
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        checkLemmaMap(pageLemmas, session, site, page.getPath() + " пустой");
        checkLemmaMap(titleLemmas, session, site, page.getPath() + " заголовок пуст");
        checkLemmaMap(bodyLemmas, session, site, page.getPath() + " тело пустое");
        session.save(page);
        Query query = session.createQuery("FROM Lemma WHERE lemma = :lemma AND siteId = :siteId");
        for(String sLemma : pageLemmas.keySet()) {
            if(Thread.interrupted()){
                site.setStatus(SiteStatus.INDEXED);
                session.update(site);
                transaction.commit();
                session.close();
                return;
            }
            float rank;
            Lemma dBLemma = saveAndGetLemma(site, query, sLemma, session);
            rank = titleLemmas.getOrDefault(sLemma, 0) * TITLE_RANK;
            rank += bodyLemmas.getOrDefault(sLemma, 0) * BODY_RANK;
            session.save(new Index(page, dBLemma, rank));
        }
        site.setDateTime(LocalDateTime.now());
        session.update(site);
        transaction.commit();
        session.close();
    }

    private static Lemma saveAndGetLemma (Site site, Query query, String sLemma, Session session) {
        Lemma dBLemma;
        Optional<Lemma> optional = query
                .setParameter("lemma", sLemma)
                .setParameter("siteId", site.getId())
                .stream()
                .findFirst();;
        if(optional.isEmpty()) {
            dBLemma = new Lemma(sLemma, 1, site.getId());
            session.save(dBLemma);
        }
        else {
            dBLemma = optional.get();
            dBLemma.setFrequency(dBLemma.getFrequency() + 1);
            session.update(dBLemma);
        }
        return dBLemma;

    }

    private static void forException (String message, Site site) {
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        long lemmasCount = (long) session.createQuery("SELECT COUNT(*) FROM Lemma WHERE siteId = :siteId")
                .setParameter("siteId", site.getId())
                .stream()
                .findFirst().get();
        if(lemmasCount == 0) {
            site.setStatus(SiteStatus.FAILED);
        }
        site.setLastError(message);
        site.setDateTime(LocalDateTime.now());
        session.update(site);
        transaction.commit();
        session.close();
    }

    private static void clearDB (Session session, Page page) {
        Transaction transaction = session.beginTransaction();
        for(Lemma lemma : page.getLemmas()) {
            int newFrequency = lemma.getFrequency() - 1;
            if(newFrequency == 0) {
                session.createQuery("DELETE FROM Lemma WHERE id = :id").setParameter("id", lemma.getId()).executeUpdate();
            }
            else {
                lemma.setFrequency(newFrequency);
                session.update(lemma);
            }
        }
        session.createQuery("DELETE FROM Page WHERE id = :id").setParameter("id", page.getId()).executeUpdate();
        transaction.commit();
    }

    private static void checkLemmaMap (HashMap<String, Integer> pageLemmas, Session session, Site site, String message) {
        if(pageLemmas.isEmpty()) {
            site.setLastError(message);
            site.setDateTime(LocalDateTime.now());
            session.update(site);
        }
    }

    private static void updateSiteTime (Site site) {
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        site.setDateTime(LocalDateTime.now());
        session.update(site);
        transaction.commit();
        session.close();

    }


}
