package com.github.eseoa.searchEngine.main.entities.repositories;

import com.github.eseoa.searchEngine.main.entities.Page;
import com.github.eseoa.searchEngine.main.entities.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PageRepository extends JpaRepository<Page, Integer> {
    long countBySiteId (int id);
    Optional<Page> findByPath (String path);

}
