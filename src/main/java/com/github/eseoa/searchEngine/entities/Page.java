package com.github.eseoa.searchEngine.entities;

import com.github.eseoa.searchEngine.lemmitization.LemmasGenerator;
import lombok.Data;
import org.jsoup.Jsoup;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "pages")
@Data

public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private int code;
    @Column(nullable = false, columnDefinition = "text")
    private String path;
    @Column(nullable = false, columnDefinition = "mediumtext")
    private String content;
    @Column(name = "site_id")
    private int siteId;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "indexes",
            joinColumns = {@JoinColumn(name = "page_id")},
            inverseJoinColumns = {@JoinColumn(name = "lemma_id")}
    )
    private List<Lemma> lemmas;

    public Page() {
    }

    public Page(int code, String path, String content, int siteId) {
        this.code = code;
        this.path = path;
        this.content = content;
        this.siteId = siteId;
    }

    public String getTitle () {
        return Jsoup.parse(content).title();
    }

    public String getSnippet (String searchString) {
        String text = Jsoup.parse(content).body().text();
        StringBuilder sb = new StringBuilder();
        String [] StringArray = LemmasGenerator.getStringArray(searchString);
        for(String s : StringArray) {
            if(text.contains(s)) {
                int wordStart = text.indexOf(s);
                int wordEnd = wordStart + s.length();
                int end = Math.min(wordEnd + 50, text.length());
                int start = Math.max(wordStart - 25, 0);
                sb.append("...")
                        .append(text.substring(start, wordStart))
                        .append("<b>")
                        .append(text.substring(wordStart, wordEnd))
                        .append("</b>")
                        .append(text.substring(wordEnd, end))
                        .append("...")
                        .append("<br>");
            }
        }
        if(sb.isEmpty()) {
            sb.append("Нет точного совпадения");
        }


        return sb.toString();
    }

    @Override
    public String toString() {
        String s = "Page{" + "id = " + id + ", path = " + path + ", siteId = " + siteId + ", lemmasCount = " + lemmas.size() + ", lemmas {";
        for(Lemma lemma : lemmas) {
            s = s + "lemmaId = " + lemma.getId() + ", lemma = " + lemma.getLemma() + ", lemmaId = " + lemma.getId() + ", siteId = " + lemma.getSiteId();
        }
        s = s + "}\n}";
        return s;
    }
}
