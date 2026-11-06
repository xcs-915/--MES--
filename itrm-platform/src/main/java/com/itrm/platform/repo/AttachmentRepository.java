package com.itrm.platform.repo;

import com.itrm.platform.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long>, JpaSpecificationExecutor<Attachment> {

    List<Attachment> findByRefTypeAndRefIdOrderByCreatedAtDesc(String refType, Long refId);
}
