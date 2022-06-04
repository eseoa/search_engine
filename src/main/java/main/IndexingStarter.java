package main;


import main.entities.Site;
import main.entities.enums.SiteStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        Site site = new Site(SiteStatus.INDEXING, LocalDateTime.now(), null, url, name);
        session.save(site);
        transaction.commit();
        session.close();
        LinksParser.setCANCEL(false);
        LinksParser linksParser = new LinksParser(url, url, site);
        forkJoinPool.submit(linksParser);
        while (true) {
           if(Thread.interrupted()) {
               forkJoinPool.shutdownNow();
               LinksParser.setCANCEL(true);
               break;
           }
           if(forkJoinPool.isQuiescent()) {
               break;
           }
        }
        try {
            Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        session = HibernateUtil.getHibernateSession();
        transaction = session.beginTransaction();
        site = (Site) session.createQuery("FROM Site WHERE id = :siteId")
                .setParameter("siteId", site.getId())
                .list()
                .get(0);
        long lemmasCount = (long) session.createQuery("SELECT COUNT(*) FROM Lemma WHERE siteId = :siteId")
                .setParameter("siteId", site.getId())
                .stream()
                .findFirst().get();
        if(lemmasCount == 0) {
            site.setStatus(SiteStatus.FAILED);
        }
        if(!site.getStatus().equals(SiteStatus.FAILED)) {
            site.setStatus(SiteStatus.INDEXED);
        }
        site.setDateTime(LocalDateTime.now());
        session.update(site);
        transaction.commit();
        session.close();
    }
}
