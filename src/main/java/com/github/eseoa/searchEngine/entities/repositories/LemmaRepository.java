package com.github.eseoa.searchEngine.entities.repositories;

import com.github.eseoa.searchEngine.entities.Lemma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma, Integer>, JpaSpecificationExecutor<Lemma> {
    long countBySiteId (int id);
    List<Lemma> findBySiteIdAndLemma(int siteId, String lemma);

    @Transactional
    @Modifying (clearAutomatically=true)
    @Query("update Lemma set frequency = :frequency where id = :id")
    int setFrequencyById(@Param("frequency") int frequency, @Param("id") int id);

}
