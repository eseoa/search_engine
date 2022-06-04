package main.responses.statistics;

import lombok.Data;
import org.hibernate.Session;
import org.hibernate.query.Query;

@Data
public class Total {
    long sites;
    long pages;
    long lemmas;
    boolean isIndexing;

    public Total(Session session) {
        Query query = session.createQuery("SELECT count(*) from Site");
        sites = (long) query.list().get(0);
        query = session.createQuery("SELECT count(*) from Page");
        pages = (long) query.list().get(0);
        query = session.createQuery("SELECT count(*) from Lemma");
        lemmas = (long) query.list().get(0);
        isIndexing = true;
    }
}
