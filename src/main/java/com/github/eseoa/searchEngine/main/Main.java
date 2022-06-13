package com.github.eseoa.searchEngine.main;

import com.github.eseoa.searchEngine.HibernateUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        HibernateUtil.getHibernateSession().close();
        SpringApplication.run(Main.class, args);
    }
}
