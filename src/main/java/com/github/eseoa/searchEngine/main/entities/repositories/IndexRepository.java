package com.github.eseoa.searchEngine.main.entities.repositories;

import com.github.eseoa.searchEngine.main.entities.Index;
import com.github.eseoa.searchEngine.main.entities.Lemma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndexRepository extends JpaRepository<Index, Integer>, JpaSpecificationExecutor<Index> {
}
