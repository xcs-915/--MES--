package com.tns.mes.identity.repo;
import com.tns.mes.identity.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> { Optional<MenuItem> findByCode(String code); }
