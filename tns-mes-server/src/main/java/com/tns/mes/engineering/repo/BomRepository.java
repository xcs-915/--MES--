package com.tns.mes.engineering.repo;

import com.tns.mes.engineering.domain.Bom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BomRepository extends JpaRepository<Bom, Long> {
    @EntityGraph(attributePaths = "items")
    Optional<Bom> findWithItemsById(Long id);
    @EntityGraph(attributePaths = "items")
    Page<Bom> findByProductId(Long productId, Pageable pageable);
    boolean existsByProductIdAndCodeAndVersionCode(Long productId, String code, String versionCode);
    Optional<Bom> findByProductIdAndCodeAndVersionCode(Long productId, String code, String versionCode);
}
