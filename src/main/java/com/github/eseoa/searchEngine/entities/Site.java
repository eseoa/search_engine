package com.github.eseoa.searchEngine.entities;

import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table (name = "sites")
@Data
public class Site {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private SiteStatus status;
    @Column (columnDefinition = "datetime", nullable = false)
    private LocalDateTime DateTime;
    @Column (columnDefinition = "text")
    private String lastError;
    @Column (nullable = false)
    private String url;
    @Column (nullable = false)
    private String name;

    public Site(SiteStatus status, LocalDateTime dateTime, String lastError, String url, String name) {
        this.status = status;
        this.DateTime = dateTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }

    public Site() {
    }
}
