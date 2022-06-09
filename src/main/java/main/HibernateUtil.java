package main;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;

public class HibernateUtil {
    private static SessionFactory sessionFactory;

    public static Session getHibernateSession() {
        if (sessionFactory == null) {

            sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
        }
        return sessionFactory.openSession();
    }

    public static Statistics getStatistics() {
        if (sessionFactory == null) {

            sessionFactory = new Configuration().configure("hibernate.cfg.xml").buildSessionFactory();
        }
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

}
