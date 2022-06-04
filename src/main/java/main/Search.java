package main;

import main.entities.Lemma;
import main.entities.Page;
import main.entities.Site;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Search {

    public static ArrayList<SearchResult> search (String searchString) {
        HashMap <String, Integer> lemmaCountMapRequest;
        List<Lemma> DBLemmas;
        HashMap<Page, Double> pageRank;
        ArrayList<SearchResult> searchResults;
        double maxAbsRank;
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        lemmaCountMapRequest = LemmasGenerator.getLemmaCountMap(searchString);
        if (lemmaCountMapRequest.isEmpty()) {
            return new ArrayList<>();
        }
        DBLemmas = getLemmasFromDB(session, lemmaCountMapRequest);
        if(DBLemmas.isEmpty()) {
            return new ArrayList<>();
        }
        pageRank = getPageRankMap(session, DBLemmas, lemmaCountMapRequest.size());
        if(pageRank.isEmpty()) {
            return new ArrayList<>();
        }
        maxAbsRank = pageRank.values().stream().max(Double::compare).get();
        searchResults = new ArrayList<>();
        for(Map.Entry<Page, Double> entry : pageRank.entrySet()) {
            String uri = entry.getKey().getPath();

            double relRank = entry.getValue() / maxAbsRank;
            String title = entry.getKey().getTitle();
            String snippet = entry.getKey().getSnippet(searchString);
            Session s = HibernateUtil.getHibernateSession();
            Site site = (Site) s.createQuery("FROM Site WHERE id = :id").setParameter("id", entry.getKey().getSiteId()).stream().findFirst().get();
            uri = uri.replaceAll(site.getUrl(),"");
            s.close();
            searchResults.add(new SearchResult(site.getUrl(), site.getName(), uri, title, snippet, relRank));
        }
        searchResults.sort((o1, o2) -> Double.compare(o2.getRelevance(), o1.getRelevance()));
        transaction.commit();
        session.close();
        return searchResults;
    }

    public static ArrayList<SearchResult> search (String searchString, String searchSite) {
        HashMap<String, Integer> lemmaCountMapRequest;
        List<Lemma> DBLemmas;
        HashMap<Page, Double> pageRank;
        ArrayList<SearchResult> searchResults;
        double maxAbsRank;
        Session session = HibernateUtil.getHibernateSession();
        Transaction transaction = session.beginTransaction();
        lemmaCountMapRequest = LemmasGenerator.getLemmaCountMap(searchString);
        if (lemmaCountMapRequest.isEmpty()) {
            return new ArrayList<>();
        }
        Site site = (Site) session.createQuery("FROM Site WHERE url = :url").setParameter("url", searchSite).stream().findFirst().get();
        DBLemmas = getLemmasFromDBBySite(session, lemmaCountMapRequest, site);
        if (DBLemmas.isEmpty()) {
            return new ArrayList<>();
        }
        pageRank = getPageRankMap(session, DBLemmas, lemmaCountMapRequest.size());
        if (pageRank.isEmpty()) {
            return new ArrayList<>();
        }
        maxAbsRank = pageRank.values().stream().max(Double::compare).get();
        searchResults = new ArrayList<>();
        for (Map.Entry<Page, Double> entry : pageRank.entrySet()) {
            String uri = entry.getKey().getPath();
            uri = uri.replaceAll(site.getUrl(),"");
            double relRank = entry.getValue() / maxAbsRank;
            String title = entry.getKey().getTitle();
            String snippet = entry.getKey().getSnippet(searchString);
            searchResults.add(new SearchResult(site.getUrl(), site.getName(), uri, title, snippet, relRank));
        }
        searchResults.sort((o1, o2) -> Double.compare(o2.getRelevance(), o1.getRelevance()));
        transaction.commit();
        session.close();
        return searchResults;
    }

    private static ArrayList<Lemma> getLemmasFromDB(Session session, HashMap <String, Integer> lemmaCountMapRequest) {
        boolean isFirst = true;
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("FROM Lemma WHERE lemma = ");
        for(String lemma : lemmaCountMapRequest.keySet()) {
            if(isFirst) {
                queryBuilder.append("'").append(lemma).append("'");
                isFirst = false;
                continue;
            }
            queryBuilder.append(" OR ").append("lemma = '").append(lemma).append("'");
        }
        queryBuilder.append(" ORDER BY frequency");
        return (ArrayList<Lemma>) session.createQuery(queryBuilder.toString()).list();

    }

    private static StringBuilder getSumRankQueryBuilder(List<Lemma> DBLemmas) {
        boolean isFirst = true;
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT SUM(i.rank) FROM Lemma AS l\n" +
                "JOIN Index AS i ON l.id = i.lemma\n" +
                "JOIN Page AS p ON p.id = i.page\n" +
                "WHERE (l.id = ");
        for(Lemma lemma : DBLemmas) {
            if (isFirst) {
                queryBuilder.append(lemma.getId());
                isFirst = false;
                continue;
            }
            queryBuilder.append(" OR l.id = ").append(lemma.getId());
        }
        queryBuilder.append(") AND p.id = ");
        return queryBuilder;
    }

    private static ArrayList<Page> getPagesByLemmas (List<Lemma> DBLemmas, int lemmasCount) {
        HashMap<Integer, ArrayList<Lemma>> siteLemmasMap = new HashMap<>();
        ArrayList<Page> pagesList = new ArrayList<>();
        boolean isFirst = true;
        for(Map.Entry<Integer, ArrayList<Lemma>> entry : getSiteLemmasMap(DBLemmas).entrySet()) {
            if(entry.getValue().size() == lemmasCount) {
                siteLemmasMap.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<Integer, ArrayList<Lemma>> entry : siteLemmasMap.entrySet())
        {
            ArrayList<Page> pages = new ArrayList<>();
            for (Lemma lemma : entry.getValue()) {
                if (isFirst) {
                    pages.addAll(lemma.getPages());
                    isFirst = false;
                    continue;
                }
                for (int i = 0; i < pages.size(); i++) {
                    if (!pages.get(i).getLemmas().stream().map(l -> l.getId()).toList().contains(lemma.getId())) {
                        pages.remove(i);
                        i--;
                    }
                }
            }
            isFirst = true;
            pagesList.addAll(pages);
    }
        return pagesList;

    }

    private static HashMap<Page, Double> getPageRankMap (Session session, List<Lemma> DBLemmas, int lemmasCount) {
        StringBuilder queryBuilder;
        ArrayList<Page> pages = getPagesByLemmas(DBLemmas, lemmasCount);
        HashMap<Page, Double> pageRank = new HashMap<>();
        for(Page page : pages){
            queryBuilder = new StringBuilder();
            queryBuilder.append(getSumRankQueryBuilder(DBLemmas)).append(page.getId());
            pageRank.put(page, (Double) session.createQuery(queryBuilder.toString()).list().get(0));
        }
        return pageRank;

    }

    private static HashMap<Integer, ArrayList<Lemma>> getSiteLemmasMap (List<Lemma> DBLemmas) {
        HashMap<Integer, ArrayList<Lemma>> siteLemmasMap = new HashMap<>();
        for (Lemma lemma : DBLemmas) {
            if (siteLemmasMap.get(lemma.getSiteId()) == null) {
                ArrayList<Lemma> l = new ArrayList<>();
                l.add(lemma);
                siteLemmasMap.put(lemma.getSiteId(), l);
            } else {
                ArrayList<Lemma> l = siteLemmasMap.get(lemma.getSiteId());
                l.add(lemma);
                siteLemmasMap.put(lemma.getSiteId(), l);
            }
        }
        return siteLemmasMap;
    }

    private static ArrayList<Lemma> getLemmasFromDBBySite(Session session,
                                                          HashMap <String, Integer> lemmaCountMapRequest,
                                                          Site site) {
        boolean isFirst = true;
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("FROM Lemma WHERE ( lemma = ");
        for(String lemma : lemmaCountMapRequest.keySet()) {
            if(isFirst) {
                queryBuilder.append("'").append(lemma).append("'");
                isFirst = false;
                continue;
            }
            queryBuilder.append(" OR ").append("lemma = '").append(lemma).append("'");
        }
        queryBuilder.append(" ) AND ").append("siteId = '").append(site.getId()).append("'");
        queryBuilder.append(" ORDER BY frequency");
        ArrayList<Lemma> lemmas = (ArrayList<Lemma>) session.createQuery(queryBuilder.toString()).list();
        if(lemmaCountMapRequest.keySet().size() != lemmas.size()) {
            lemmas.clear();
        }
        return lemmas;

    }

}
