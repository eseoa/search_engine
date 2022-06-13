package com.github.eseoa.searchEngine.entities;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "fields")
@Data
public class Field {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String  selector;
    @Column(nullable = false)
    private float weight;
}
