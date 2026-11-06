package com.itrm.platform.web;

import com.itrm.platform.common.ApiResponse;
import com.itrm.platform.domain.AttachmentMetadata;
import com.itrm.platform.domain.RequirementAttachment;
import com.itrm.platform.service.RequirementAttachmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/requirements/{requirementId}/attachments")
public class RequirementAttachmentController {

    private final RequirementAttachmentService attachmentService;

    public RequirementAttachmentController(RequirementAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public ApiResponse<List<AttachmentMetadata>> list(@PathVariable Long requirementId) {
        return ApiResponse.ok(attachmentService.list(requirementId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('requirement:edit')")
    public ApiResponse<RequirementAttachment> upload(@PathVariable Long requirementId,
                                                     @RequestPart("file") MultipartFile file) {
        RequirementAttachment saved = attachmentService.upload(requirementId, file);
        saved.setFileData(null);
        return ApiResponse.ok(saved);
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long requirementId,
                                          @PathVariable Long attachmentId,
                                          @RequestParam(defaultValue = "false") boolean download) {
        RequirementAttachment attachment = attachmentService.get(requirementId, attachmentId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getContentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = (download || !Boolean.TRUE.equals(attachment.getImage())
                ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(attachment.getFileData());
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAuthority('requirement:edit')")
    public ApiResponse<Void> delete(@PathVariable Long requirementId,
                                    @PathVariable Long attachmentId) {
        attachmentService.delete(requirementId, attachmentId);
        return ApiResponse.ok();
    }
}

