package com.itrm.platform.repo;

import com.itrm.platform.domain.AttachmentMetadata;
import com.itrm.platform.domain.RequirementAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequirementAttachmentRepository extends JpaRepository<RequirementAttachment, Long> {
    @Query("select new com.itrm.platform.domain.AttachmentMetadata(a.id, a.requirementId, a.fileName, " +
            "a.contentType, a.fileSize, a.image, a.uploadedBy, a.uploadedAt) " +
            "from RequirementAttachment a where a.requirementId = :requirementId order by a.uploadedAt desc")
    List<AttachmentMetadata> findMetadataByRequirementId(@Param("requirementId") Long requirementId);
    Optional<RequirementAttachment> findByIdAndRequirementId(Long id, Long requirementId);
}

