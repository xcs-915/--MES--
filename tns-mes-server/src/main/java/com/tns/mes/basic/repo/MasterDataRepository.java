package com.tns.mes.basic.repo;

import com.tns.mes.basic.domain.MasterData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterDataRepository extends JpaRepository<MasterData, Long> {
    Page<MasterData> findByDataTypeAndCodeContainingIgnoreCase(String dataType, String code, Pageable pageable);
    Page<MasterData> findByDataType(String dataType, Pageable pageable);
    Optional<MasterData> findByDataTypeAndCode(String dataType, String code);
    boolean existsByDataTypeAndCode(String dataType, String code);
}

