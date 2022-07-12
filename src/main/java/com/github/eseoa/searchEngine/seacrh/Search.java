package com.github.eseoa.searchEngine.seacrh;


import com.github.eseoa.searchEngine.entities.Index;
import com.github.eseoa.searchEngine.entities.Lemma;
import com.github.eseoa.searchEngine.entities.Page;
import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.repositories.IndexRepository;
import com.github.eseoa.searchEngine.entities.repositories.LemmaRepository;
import com.github.eseoa.searchEngine.entities.repositories.SiteRepository;
import com.github.eseoa.searchEngine.entities.*;
import com.github.eseoa.searchEngine.lemmitization.LemmasGenerator;
import com.github.eseoa.searchEngine.entities.repositories.specs.IndexSpecification;
import com.github.eseoa.searchEngine.entities.repositories.specs.LemmaSpecification;
import com.github.eseoa.searchEngine.entities.repositories.specs.SearchCriteria;
import com.github.eseoa.searchEngine.entities.repositories.specs.SearchOperation;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;


public class Search {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private HashMap<String, Integer> lemmaCountMapRequest;
    private List<Lemma> dBLemmas;
    private HashMap<Page, Double> pageRank;
    private ArrayList<SearchResult> searchResults;
    private double maxAbsRank;



    public Search(SiteRepository siteRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public ArrayList<SearchResult> search (String searchString) {
        try {
            lemmaCountMapRequest = LemmasGenerator.getLemmaCountMap(searchString);
            if (lemmaCountMapRequest.isEmpty()) {
                return new ArrayList<>();
            }
            dBLemmas = getLemmasFromDB();
            if (dBLemmas.isEmpty()) {
                return new ArrayList<>();
            }
            pageRank = getPageRankMap();
            if (pageRank.isEmpty()) {
                return new ArrayList<>();
            }
            maxAbsRank = pageRank.values().stream().max(Double::compare).get();
            searchResults = new ArrayList<>();
            for (Map.Entry<Page, Double> entry : pageRank.entrySet()) {
                String uri = entry.getKey().getPath();
                double relRank = entry.getValue() / maxAbsRank;
                String title = entry.getKey().getTitle();
                String snippet = entry.getKey().getSnippet(searchString);
                Site site = siteRepository.findById(entry.getKey().getSiteId()).get();
                uri = uri.replaceAll(site.getUrl(), "");
                searchResults.add(new SearchResult(site.getUrl(), site.getName(), uri, title, snippet, relRank));
            }
            searchResults.sort((o1, o2) -> Double.compare(o2.getRelevance(), o1.getRelevance()));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return searchResults;
    }

    public ArrayList<SearchResult> search (String searchString, String searchSite) {
        try {
            lemmaCountMapRequest = LemmasGenerator.getLemmaCountMap(searchString);
            if (lemmaCountMapRequest.isEmpty()) {
                return new ArrayList<>();
            }
            Optional<Site> site = siteRepository.findByUrl(searchSite);
            if(site.isEmpty()) {
                return new ArrayList<>();
            }
            dBLemmas = getLemmasFromDBBySite(site.get());
            if (dBLemmas.isEmpty()) {
                return new ArrayList<>();
            }
            pageRank = getPageRankMap();
            if (pageRank.isEmpty()) {
                return new ArrayList<>();
            }
            maxAbsRank = pageRank.values().stream().max(Double::compare).get();
            searchResults = new ArrayList<>();
            for (Map.Entry<Page, Double> entry : pageRank.entrySet()) {
                String uri = entry.getKey().getPath();
                uri = uri.replaceAll(site.get().getUrl(), "");
                double relRank = entry.getValue() / maxAbsRank;
                String title = entry.getKey().getTitle();
                String snippet = entry.getKey().getSnippet(searchString);
                searchResults.add(new SearchResult(site.get().getUrl(), site.get().getName(), uri, title, snippet, relRank));
            }
            searchResults.sort((o1, o2) -> Double.compare(o2.getRelevance(), o1.getRelevance()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return searchResults;
    }

    private List<Lemma> getLemmasFromDB() {
        LemmaSpecification lemmaSpecification = new LemmaSpecification();
        for(String lemma : lemmaCountMapRequest.keySet()) {
            lemmaSpecification.add(new SearchCriteria("lemma", lemma, SearchOperation.EQUAL));
        }
        List<Lemma> dBLemmas= lemmaRepository.findAll(lemmaSpecification);
        dBLemmas.sort(Comparator.comparingInt(Lemma::getFrequency));
        return dBLemmas;
    }

    private HashMap<Page, Double> getPageRankMap () {
        ArrayList<Page> pages = getPagesByLemmas();
        HashMap<Page, Double> pageRank = new HashMap<>();
        IndexSpecification indexSpecLemmas = getIndexSpecForDBLemmas();
        for(Page page : pages) {
            IndexSpecification indexSpecPage = new IndexSpecification();
            indexSpecPage.add(new SearchCriteria("page", page, SearchOperation.EQUAL));
            double rank = indexRepository
                    .findAll(Specification.where(indexSpecLemmas).and(indexSpecPage)).stream()
                    .map(Index::getRank)
                    .reduce(0.0, Double::sum);

            pageRank.put(page, rank);
        }
        return pageRank;
    }

    private ArrayList<Page> getPagesByLemmas () {
        HashMap<Integer, ArrayList<Lemma>> siteIdLemmasMap = getSiteIdLemmasMap();
        ArrayList<Page> pagesList = new ArrayList<>();
        boolean isFirst = true;
        for (Map.Entry<Integer, ArrayList<Lemma>> entry : siteIdLemmasMap.entrySet())
        {
            ArrayList<Page> pages = new ArrayList<>();
            for (Lemma lemma : entry.getValue()) {
                if (isFirst) {
                    pages.addAll(lemma.getPages());
                    isFirst = false;
                    continue;
                }
                delPagesWithoutLemma(pages, lemma);
            }
            isFirst = true;
            pagesList.addAll(pages);
        }
        return pagesList;

    }

    private IndexSpecification getIndexSpecForDBLemmas() {
        IndexSpecification indexSpecification = new IndexSpecification();
        for(Lemma lemma : dBLemmas) {
            indexSpecification.add(new SearchCriteria("lemma", lemma, SearchOperation.EQUAL));
        }
        return indexSpecification;
    }

    private HashMap<Integer, ArrayList<Lemma>> getSiteIdLemmasMap () {
        HashMap<Integer, ArrayList<Lemma>> siteIdLemmasMap = new HashMap<>();
        for (Lemma lemma : dBLemmas) {
            if (siteIdLemmasMap.get(lemma.getSiteId()) == null) {
                ArrayList<Lemma> l = new ArrayList<>();
                l.add(lemma);
                siteIdLemmasMap.put(lemma.getSiteId(), l);
            } else {
                ArrayList<Lemma> l = siteIdLemmasMap.get(lemma.getSiteId());
                l.add(lemma);
                siteIdLemmasMap.put(lemma.getSiteId(), l);
            }
        }
        siteIdLemmasMap.entrySet().removeIf(entry -> entry.getValue().size() != lemmaCountMapRequest.size());
        return siteIdLemmasMap;
    }

    private List<Lemma> getLemmasFromDBBySite(Site site) {
        LemmaSpecification lemmaSpecification = new LemmaSpecification();
        for(String lemma : lemmaCountMapRequest.keySet()) {
            lemmaSpecification.add(new SearchCriteria("lemma", lemma, SearchOperation.EQUAL));
        }
        LemmaSpecification lemmaSpecSite = new LemmaSpecification();
        lemmaSpecSite.add(new SearchCriteria ("siteId", site.getId(), SearchOperation.EQUAL));
        List<Lemma> dBLemmas= lemmaRepository.findAll(Specification.where(lemmaSpecification).and(lemmaSpecSite));
        if(lemmaCountMapRequest.keySet().size() != dBLemmas.size()) {
            dBLemmas.clear();;
        }
        return dBLemmas;

    }

    private void delPagesWithoutLemma (List<Page> pages, Lemma lemma) {
        for (int i = 0; i < pages.size(); i++) {
            if (!pages.get(i).getLemmas().stream().map(Lemma::getId).toList().contains(lemma.getId())) {
                pages.remove(i);
                i--;
            }
        }

    }

}