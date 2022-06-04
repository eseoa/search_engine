package main;

import main.entities.Site;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

public class  LinksParser extends RecursiveTask<CopyOnWriteArraySet<String>> {

    private String url;
    private CopyOnWriteArraySet<String> linksList = new CopyOnWriteArraySet<>();
    private final String startUrl;
    private final Site site;
    private static boolean CANCEL = false;


    public LinksParser(String url, String startUrl, Site site) {
        this.site = site;
        this.url = url;
        this.startUrl = startUrl;
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
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        for(LinksParser task : taskList){
            task.join();
        }
        return linksList;
    }

    private void addLinkToList(Elements elements, List<LinksParser> taskList){
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

    private void parsePage(List<LinksParser> taskList) throws InterruptedException, IOException {
        if(Thread.interrupted()){
            return;
        }
        Thread.sleep(500);
        Elements elements = PageParser.parse(url, site);
        if(elements == null){
            return;
        }
        addLinkToList(elements, taskList);

    }

    public static void setCANCEL(boolean cancel) {
        LinksParser.CANCEL = cancel;
    }
}
