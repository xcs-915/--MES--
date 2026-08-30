package com.tns.mes.identity.repo;
import com.tns.mes.identity.domain.DataDictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DataDictionaryRepository extends JpaRepository<DataDictionary, Long> { List<DataDictionary> findByDictTypeOrderBySortOrderAsc(String dictType); }
