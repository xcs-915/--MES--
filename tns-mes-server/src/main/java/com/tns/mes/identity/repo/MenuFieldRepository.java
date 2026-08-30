package com.tns.mes.identity.repo;

import com.tns.mes.identity.domain.MenuField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuFieldRepository extends JpaRepository<MenuField, Long> {
    List<MenuField> findByMenuCodeOrderBySortOrderAsc(String menuCode);
}
