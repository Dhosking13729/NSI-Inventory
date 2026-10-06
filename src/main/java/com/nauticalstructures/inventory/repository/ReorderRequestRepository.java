package com.nauticalstructures.inventory.repository;

import com.nauticalstructures.inventory.domain.ReorderRequest;
import com.nauticalstructures.inventory.domain.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReorderRequestRepository extends JpaRepository<ReorderRequest, Integer> {

    @EntityGraph(attributePaths = {"material", "alert", "reviewedBy"})
    List<ReorderRequest> findByRequestStatusInOrderByCreatedDateDescRequestIdDesc(Collection<RequestStatus> statuses);

    @EntityGraph(attributePaths = {"material", "alert", "reviewedBy"})
    List<ReorderRequest> findTop20ByRequestStatusOrderByCreatedDateDescRequestIdDesc(RequestStatus status);

    Optional<ReorderRequest> findByAlertAlertId(Integer alertId);

    @Override
    @EntityGraph(attributePaths = {"material", "alert", "reviewedBy"})
    Optional<ReorderRequest> findById(Integer id);
}
