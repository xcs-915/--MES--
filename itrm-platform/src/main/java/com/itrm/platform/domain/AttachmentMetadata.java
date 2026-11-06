package com.itrm.platform.domain;

import java.time.Instant;

public class AttachmentMetadata {
    private final Long id;
    private final Long requirementId;
    private final String fileName;
    private final String contentType;
    private final Long fileSize;
    private final Boolean image;
    private final String uploadedBy;
    private final Instant uploadedAt;

    public AttachmentMetadata(Long id, Long requirementId, String fileName, String contentType,
                              Long fileSize, Boolean image, String uploadedBy, Instant uploadedAt) {
        this.id = id;
        this.requirementId = requirementId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.image = image;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public Long getRequirementId() { return requirementId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public Boolean getImage() { return image; }
    public String getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
}

