package com.github.eseoa.searchEngine.parser;

import com.github.eseoa.searchEngine.main.entities.Site;
import com.github.eseoa.searchEngine.main.entities.repositories.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

public class  LinksParser extends RecursiveTask<CopyOnWriteArraySet<String>> {

    private final String url;
    private CopyOnWriteArraySet<String> linksList = new CopyOnWriteArraySet<>();
    private final String startUrl;
    private final Site site;
    private static boolean CANCEL = false;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;

    public LinksParser(String url, String startUrl, Site site,
                       SiteRepository siteRepository,
                       LemmaRepository lemmaRepository,
                       IndexRepository indexRepository,
                       PageRepository pageRepository ) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.pageRepository = pageRepository;
        this.site = site;
        this.url = url;
        this.startUrl = startUrl;
        if (linksList.isEmpty()){
            linksList.add(url);
        }
    }

    private LinksParser(String url, String startUrl, Site site, CopyOnWriteArraySet<String> linksList,
                        SiteRepository siteRepository,
                        LemmaRepository lemmaRepository,
                        IndexRepository indexRepository,
                        PageRepository pageRepository ) {
        this(url, startUrl, site, siteRepository, lemmaRepository, indexRepository, pageRepository);
        this.linksList = linksList;
    }

    @Override
    protected CopyOnWriteArraySet<String> compute() {
        List<LinksParser> taskList = new ArrayList<>();
        try {
            parsePage(taskList);
            for(LinksParser task : taskList){
                task.join();
            }
        } catch (InterruptedException ignored) {}
        return linksList;
    }

    private void addLinkToList(Elements elements, List<LinksParser> taskList) {
        for (Element element : elements){
            if(Thread.interrupted()){
                return;
            }
            String link = element.attr("abs:href");
            if(!linksList.contains(link) && !link.isEmpty() && !link.contains("#") && link.startsWith(startUrl)){
                LinksParser linkParser = new LinksParser(link, startUrl, site, linksList,
                        siteRepository,
                        lemmaRepository,
                        indexRepository,
                        pageRepository);
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
        try {
            if (Thread.interrupted()) {
                return;
            }
            Thread.sleep(5000);
            Elements elements = new PageParser(siteRepository,
                    lemmaRepository,
                    indexRepository,
                    pageRepository,
                    site.getId(),
                    url).parse();
            if (elements == null) {
                return;
            }
            addLinkToList(elements, taskList);
        }
        catch (InterruptedException ignored) {}
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void setCANCEL(boolean cancel) {
        LinksParser.CANCEL = cancel;
    }
}