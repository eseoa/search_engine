package main.responses.statistics;

import lombok.Data;
import main.HibernateUtil;
import main.entities.Site;
import org.hibernate.Session;

import java.util.ArrayList;

@Data
public class Statistics {
    Total total;
    ArrayList<Detailed> detailed = new ArrayList<>();

    public Statistics(Session session) {
        total = new Total(session);
        ArrayList<Site> sites = (ArrayList<Site>) session.createQuery("from Site").list();
        for(Site site : sites) {
            detailed.add(new Detailed(session, site));
        }
        session.close();
    }
}
