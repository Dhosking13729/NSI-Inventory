package com.nauticalstructures.inventory.repository;

import com.nauticalstructures.inventory.domain.InventoryTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Finders used by pages fetch material and staff eagerly; open-in-view is off. */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {

    @EntityGraph(attributePaths = {"material", "staff"})
    List<InventoryTransaction> findTop15ByOrderByTransactionTimestampDescTransactionIdDesc();

    @EntityGraph(attributePaths = {"material", "staff"})
    List<InventoryTransaction> findByMaterialMaterialIdOrderByTransactionTimestampDescTransactionIdDesc(Integer materialId);
}
