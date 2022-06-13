package com.github.eseoa.searchEngine.entities;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Entity
@Table (name = "lemmas")
@Data
public class Lemma {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @Column (nullable = false)
    private String lemma;
    @Column (nullable = false)
    private int frequency;
    @Column(name = "site_id")
    private int siteId;
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "indexes",
            joinColumns = {@JoinColumn(name = "lemma_id")},
            inverseJoinColumns = {@JoinColumn(name = "page_id")}
    )
    private List<Page> pages;

    public Lemma(String lemma, int frequency, int siteId) {
        this.siteId = siteId;
        this.lemma = lemma;
        this.frequency = frequency;
    }

    public Lemma() {
    }

    @Override
    public String toString() {
        String s = "Lemma{" + "id = " + id + ",  lemma = " + lemma + ", frequency = " + frequency + ", siteId = " + siteId + ", pagesCount = " + pages.size() + ", pages{";
        for(Page page: pages) {
            s = s + "pageId = " + page.getId() + ", path = " + page.getPath() + ", siteId = " + page.getSiteId() + ", pageCode = " + page.getCode();
        }
        s = s + "}\n}";
        return s;
    }
}
