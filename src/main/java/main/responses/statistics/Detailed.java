package main.responses.statistics;

import lombok.Data;
import main.entities.Site;
import org.hibernate.Session;

@Data
public class Detailed {
    private String url;
    private String name;
    private String status;
    private String statusTime;
    private String error;
    private long pages;
    private long lemmas;


    public Detailed(Session session, Site site) {
        url = site.getUrl();
        name = site.getName();
        status = String.valueOf(site.getStatus());
        statusTime = String.valueOf(site.getDateTime());
        error = site.getLastError();
        pages = (long) session.createQuery("SELECT count(*) from Page WHERE siteId = " + site.getId()).list().get(0);
        lemmas = (long) session.createQuery("SELECT count(*) from Lemma WHERE siteId = " + site.getId()).list().get(0);;


    }
}
