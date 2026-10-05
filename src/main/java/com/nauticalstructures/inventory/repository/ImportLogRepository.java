package com.nauticalstructures.inventory.repository;

import com.nauticalstructures.inventory.domain.ImportLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportLogRepository extends JpaRepository<ImportLog, Integer> {
    @EntityGraph(attributePaths = "importedBy")
    List<ImportLog> findAllByOrderByImportDateDesc();
}
