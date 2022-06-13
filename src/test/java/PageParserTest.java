import com.github.eseoa.searchEngine.HibernateUtil;
import com.github.eseoa.searchEngine.entities.Page;
import com.github.eseoa.searchEngine.entities.Site;
import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.parser.PageParser;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

public class PageParserTest {

    private String path = "https://skillbox.ru/";
    private String siteName = "skillbox";

    @Test
    @DisplayName("тест парсинга страницы")
    public void parse () {
        Session session = HibernateUtil.getHibernateSession();
        PageParser.userAgent = "Heliosbot/0.1 (+https://github.com/eseoa)";
        Optional<Site> mainSite = session.createQuery("FROM Site WHERE url = :url ")
                .setParameter("url", path)
                .stream()
                .findFirst();
        if(mainSite.isPresent()) {
            Site site = mainSite.get();
            new PageParser(session, path, site.getId()).parse();
        }
        else {
            Transaction transaction = session.beginTransaction();
            Site site = new Site(SiteStatus.INDEXED, LocalDateTime.now(), null, path, siteName);
            session.save(site);
            transaction.commit();
            new PageParser(session, path, site.getId()).parse();
        }
        Page page = (Page) session.createQuery("FROM Page WHERE path = :path")
                .setParameter("path", path)
                .stream()
                .findFirst()
                .get();
        String expected = "200 https://skillbox.ru/";
        String actual = page.getCode() + " " + page.getPath();
        Assertions.assertEquals(expected, actual);
    }
}
