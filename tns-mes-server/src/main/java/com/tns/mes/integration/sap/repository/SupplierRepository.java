package com.tns.mes.integration.sap.repository;

import com.tns.mes.integration.sap.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByBpNumber(String bpNumber);
}
