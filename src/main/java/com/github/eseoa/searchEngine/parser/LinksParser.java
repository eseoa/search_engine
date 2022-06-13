package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.HibernateUtil;
import com.github.eseoa.searchEngine.entities.Site;
import org.hibernate.Session;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

public class  LinksParser extends RecursiveTask<CopyOnWriteArraySet<String>> {

    private String url;
    private Session session;
    private CopyOnWriteArraySet<String> linksList = new CopyOnWriteArraySet<>();
    private final String startUrl;
    private Site site;
    private static boolean CANCEL = false;

    public LinksParser(String url, String startUrl, Site site) {
        this.site = site;
        this.url = url;
        this.startUrl = startUrl;
        this.session = session;
        if (linksList.isEmpty()){
            linksList.add(url);
        }
    }

    public LinksParser(String url, String startUrl, Site site, CopyOnWriteArraySet<String> linksList) {
        this(url, startUrl, site);
        this.linksList = linksList;
    }

    @Override
    protected CopyOnWriteArraySet<String> compute() {
        List<LinksParser> taskList = new ArrayList<>();
        try {
            parsePage(taskList);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(LinksParser task : taskList){
            task.join();
        }
        return linksList;
    }

    private void addLinkToList(Elements elements, List<LinksParser> taskList) {
        for (Element element : elements){
            if(Thread.interrupted()){
                return;
            }
            String link = element.attr("abs:href");
            if(!linksList.contains(link) && !link.isEmpty() && !link.contains("#") && link.startsWith(startUrl)){
                LinksParser linkParser = new LinksParser(link, startUrl, site, linksList);
                linkParser.fork();
                linksList.add(link);
                taskList.add(linkParser);
                if(CANCEL) {
                    linkParser.cancel(true);
                    taskList.forEach(linksParser -> linksParser.cancel(true));
                    taskList.clear();
                }
            }
        }
    }

    private void parsePage(List<LinksParser> taskList) throws InterruptedException {
        while (HibernateUtil.getStatistics().getSessionOpenCount() - HibernateUtil.getStatistics().getSessionCloseCount() >= 20) {
            Thread.sleep(500);
        }
        try (Session session = HibernateUtil.getHibernateSession()) {
            if (Thread.interrupted()) {
                return;
            }
            Thread.sleep(500);
            Elements elements = new PageParser(session, url, site.getId()).parse();
            if (elements == null) {
                return;
            }
            session.close();
            addLinkToList(elements, taskList);
        }
        catch (Exception e) {
            long t1 = HibernateUtil.getStatistics().getSessionOpenCount() - HibernateUtil.getStatistics().getSessionCloseCount();
            System.out.println("EXC О - З " + t1);
            System.out.println("EXC Открытых " +HibernateUtil.getStatistics().getSessionOpenCount());
            System.out.println("EXC Закрытых " + HibernateUtil.getStatistics().getSessionCloseCount());
            e.printStackTrace();
        }

    }

    public static void setCANCEL(boolean cancel) {
        LinksParser.CANCEL = cancel;
    }
}