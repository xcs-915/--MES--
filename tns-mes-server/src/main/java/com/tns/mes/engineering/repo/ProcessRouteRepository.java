package com.tns.mes.engineering.repo;

import com.tns.mes.engineering.domain.ProcessRoute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessRouteRepository extends JpaRepository<ProcessRoute, Long> {
    @EntityGraph(attributePaths = "operations")
    Optional<ProcessRoute> findWithOperationsById(Long id);
    @EntityGraph(attributePaths = "operations")
    Page<ProcessRoute> findByProductId(Long productId, Pageable pageable);
    boolean existsByProductIdAndCodeAndVersionCode(Long productId, String code, String versionCode);
    Optional<ProcessRoute> findByProductIdAndCodeAndVersionCode(Long productId, String code, String versionCode);
}
