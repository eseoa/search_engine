package com.github.eseoa.searchEngine.entities;

import lombok.Data;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.List;

@Entity
@Table (name = "lemmas", uniqueConstraints = { @UniqueConstraint(columnNames = { "lemma", "site_id" })})
@SQLInsert(sql="INSERT INTO lemmas (frequency, lemma, site_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE frequency = frequency + 1")
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
    @ManyToMany()
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
    String s = "Lemma{" + "id = " + id + ",  lemma = " + lemma +"}";
    return s;
}
}
