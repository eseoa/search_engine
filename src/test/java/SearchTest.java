//import com.github.eseoa.searchEngine.HibernateUtil;
//import com.github.eseoa.searchEngine.main.entities.Page;
//import com.github.eseoa.searchEngine.main.entities.Site;
//import com.github.eseoa.searchEngine.main.entities.enums.SiteStatus;
//import com.github.eseoa.searchEngine.parser.PageParser;
//import com.github.eseoa.searchEngine.seacrh.Search;
//import org.hibernate.Session;
//import org.hibernate.Transaction;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//public class SearchTest {
//    private String path = "https://skillbox.ru/";
//    private String siteName = "skillbox";
//
//    @BeforeEach
//    private void before() {
//        Session session = HibernateUtil.getHibernateSession();
//        PageParser.userAgent ="Heliosbot/0.1 (+https://github.com/eseoa)";
//        Optional<Site> mainSite = session.createQuery("FROM Site WHERE url = :url ")
//                .setParameter("url", path)
//                .stream()
//                .findFirst();
//        if(mainSite.isPresent()) {
//            Site site = mainSite.get();
//            new PageParser(session, path, site.getId()).parse();
//            session.close();
//        }
//        else {
//            Transaction transaction = session.beginTransaction();
//            Site site = new Site(SiteStatus.INDEXED, LocalDateTime.now(), null, path, siteName);
//            session.save(site);
//            transaction.commit();
//            session.close();
//            new PageParser(session, path, site.getId()).parse();
//        }
//    }
//
//    @Test
//    @DisplayName("поиск по всем сайтам")
//    public void fullSearch () {
//        Session session = HibernateUtil.getHibernateSession();
//        Page page = (Page) session.createQuery("FROM Page WHERE path = :path")
//                .setParameter("path", path)
//                .stream()
//                .findFirst()
//                .get();
//        session.close();
//        String expected = "skillbox";
//        Document document = Jsoup.parse(page.getContent());
//        String actual = Search.search(document.body().text()).get(0).getSiteName();
//        Assertions.assertEquals(expected, actual);
//    }
//
//    @Test
//    @DisplayName("поиск по конкретному сайту")
//    public void siteSearch () {
//        Session session = HibernateUtil.getHibernateSession();
//        Page page = (Page) session.createQuery("FROM Page WHERE path = :path")
//                .setParameter("path", path)
//                .stream()
//                .findFirst()
//                .get();
//        session.close();
//        String expected = "skillbox";
//        Document document = Jsoup.parse(page.getContent());
//        String actual = Search.search(document.body().text(), path).get(0).getSiteName();
//        Assertions.assertEquals(expected, actual);
//    }
//}
