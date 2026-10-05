package com.nauticalstructures.inventory.repository;

import com.nauticalstructures.inventory.domain.AlertStatus;
import com.nauticalstructures.inventory.domain.LowStockAlert;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Integer> {

    boolean existsByMaterialMaterialIdAndStatusIn(Integer materialId, Collection<AlertStatus> statuses);

    long countByStatus(AlertStatus status);

    @EntityGraph(attributePaths = {"material", "notifiedStaff"})
    List<LowStockAlert> findByStatusInOrderByAlertTimestampDescAlertIdDesc(Collection<AlertStatus> statuses);

    @EntityGraph(attributePaths = {"material", "notifiedStaff"})
    List<LowStockAlert> findTop20ByStatusOrderByAlertTimestampDescAlertIdDesc(AlertStatus status);

    @EntityGraph(attributePaths = {"material", "notifiedStaff"})
    List<LowStockAlert> findByMaterialMaterialIdOrderByAlertTimestampDescAlertIdDesc(Integer materialId);

    @Override
    @EntityGraph(attributePaths = {"material", "notifiedStaff"})
    Optional<LowStockAlert> findById(Integer id);
}
