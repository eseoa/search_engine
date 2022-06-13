package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.HibernateUtil;
import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import org.hibernate.Session;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;

public class IndexingStarter implements Runnable {


    private static final int WAIT_TIME = 5_000;
    private String url;
    private String name;


    public IndexingStarter(String url, String name) {

        this.url = url;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            Session session = HibernateUtil.getHibernateSession();
            session.beginTransaction();
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, url, name);
            session.save(site);
            LinksParser.setCANCEL(false);
            LinksParser linksParser = new LinksParser(url, url, site);
            forkJoinPool.submit(linksParser);
            session.getTransaction().commit();
            session.close();
            while (true) {
                if (Thread.interrupted()) {
                    forkJoinPool.shutdownNow();
                    LinksParser.setCANCEL(true);
                    break;
                }
                if (forkJoinPool.isQuiescent()) {
                    break;
                }
            }
            session = HibernateUtil.getHibernateSession();
            session.beginTransaction();
            Thread.sleep(WAIT_TIME);
            site = session.get(Site.class, site.getId());
            long lemmasCount = (long) session.createQuery("SELECT COUNT(*) FROM Lemma WHERE siteId = :siteId")
                    .setParameter("siteId", site.getId())
                    .stream()
                    .findFirst().get();
            if (lemmasCount == 0) {
                site.setStatus(SiteStatus.FAILED);
            }
            if (!site.getStatus().equals(SiteStatus.FAILED)) {
                site.setStatus(SiteStatus.INDEXED);
            }
            site.setDateTime(LocalDateTime.now());
            session.update(site);
            session.getTransaction().commit();
            session.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }
}