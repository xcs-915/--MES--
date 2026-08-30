package com.tns.mes.engineering.repo;

import com.tns.mes.engineering.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByCode(String code);
    boolean existsByCode(String code);
    Page<Product> findByCodeContainingIgnoreCaseOrNameZhContainingIgnoreCase(String code, String name, Pageable pageable);
}
