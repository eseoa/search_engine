package com.github.eseoa.searchEngine.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;

//hibernate не дает сделать таблицу "index", как это пофиксить не знаю
@Entity
@Table(name = "indexes")
@Data

@AllArgsConstructor
public class Index {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;
    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;
    @Column(nullable = false, name = "ranked")//hibernate не дает сделать поле "rank", как это пофиксить не знаю
    private double rank;

    public Index(Page page, Lemma lemma, double rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }


    public Index() {
    }
}
