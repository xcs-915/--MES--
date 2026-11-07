package com.itrm.platform.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItrmDepartmentRepository extends JpaRepository<ItrmDepartment, Long> {
    Optional<ItrmDepartment> findByFullPath(String fullPath);
    boolean existsByFullPath(String fullPath);
    List<ItrmDepartment> findAllByOrderByFullPathAsc();
}
