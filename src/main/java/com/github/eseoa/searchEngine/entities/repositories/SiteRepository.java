package com.github.eseoa.searchEngine.entities.repositories;

import com.github.eseoa.searchEngine.entities.enums.SiteStatus;
import com.github.eseoa.searchEngine.entities.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Cacheable(false)
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl (String url);
    boolean existsByUrl (String url);
    List<Site> findAllByStatus (SiteStatus siteStatus);

    @Transactional
    @Modifying (clearAutomatically=true)
    @Query("update Site set status = :status where id = :id")
    int setStatusById(@Param("status")SiteStatus siteStatus, @Param("id") int id);

    @Transactional
    @Modifying (clearAutomatically=true)
    @Query("update Site set dateTime = :dateTime where id = :id")
    int setTimeById(@Param("dateTime")LocalDateTime dateTime, @Param("id") int id);

    @Transactional
    @Modifying (clearAutomatically=true)
    @Query("update Site set lastError = :error where id = :id")
    int setErrorById(@Param("error")String error, @Param("id") int id);

    @Transactional
    @Modifying (clearAutomatically=true)
    @Query("update Site set dateTime = :dateTime, lastError = :error where id = :id")
    int setTimeAndErrorById(@Param("dateTime")LocalDateTime dateTime,
                            @Param("error")String error,
                            @Param("id") int id);

    @Transactional
    @Modifying (clearAutomatically=true)
    @Query("update Site set dateTime = :dateTime, status = :status where id = :id")
    int setTimeAndStatusById(@Param("dateTime")LocalDateTime dateTime,
                            @Param("status") SiteStatus siteStatus,
                            @Param("id") int id);
}
