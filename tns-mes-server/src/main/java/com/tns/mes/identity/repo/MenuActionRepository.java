package com.tns.mes.identity.repo;

import com.tns.mes.identity.domain.MenuAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuActionRepository extends JpaRepository<MenuAction, Long> {
    List<MenuAction> findByMenuCodeOrderBySortOrderAsc(String menuCode);
}
