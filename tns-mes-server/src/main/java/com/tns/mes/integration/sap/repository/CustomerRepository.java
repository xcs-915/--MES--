package com.tns.mes.integration.sap.repository;

import com.tns.mes.integration.sap.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByBpNumber(String bpNumber);
}
