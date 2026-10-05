package com.nauticalstructures.inventory.repository;

import com.nauticalstructures.inventory.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Integer> {
    Optional<Material> findByBarcodeValueIgnoreCase(String barcodeValue);
    List<Material> findAllByOrderByMaterialNameAsc();
}
