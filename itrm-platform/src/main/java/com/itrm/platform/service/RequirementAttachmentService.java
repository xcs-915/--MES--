package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.AttachmentMetadata;
import com.itrm.platform.domain.RequirementAttachment;
import com.itrm.platform.repo.RequirementAttachmentRepository;
import com.itrm.platform.repo.RequirementRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RequirementAttachmentService {

    public static final long MAX_FILE_SIZE = 20L * 1024L * 1024L;
    private static final Set<String> BLOCKED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "exe", "com", "bat", "cmd", "ps1", "sh", "msi", "dll", "jar", "war"
    ));

    private final RequirementAttachmentRepository attachmentRepository;
    private final RequirementRepository requirementRepository;

    public RequirementAttachmentService(RequirementAttachmentRepository attachmentRepository,
                                        RequirementRepository requirementRepository) {
        this.attachmentRepository = attachmentRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional(readOnly = true)
    public List<AttachmentMetadata> list(Long requirementId) {
        ensureRequirement(requirementId);
        return attachmentRepository.findMetadataByRequirementId(requirementId);
    }

    @Transactional(readOnly = true)
    public RequirementAttachment get(Long requirementId, Long attachmentId) {
        ensureRequirement(requirementId);
        RequirementAttachment attachment = attachmentRepository.findByIdAndRequirementId(attachmentId, requirementId)
                .orElseThrow(() -> new BizException(404, "附件不存在"));
        if (attachment.getFileData() == null) {
            throw new BizException(404, "该历史附件缺少文件内容，请重新上传");
        }
        return attachment;
    }

    @Transactional
    public RequirementAttachment upload(Long requirementId, MultipartFile file) {
        ensureRequirement(requirementId);
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(400, "单个文件不能超过 20MB");
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
        if (fileName.contains("..")) {
            throw new BizException(400, "文件名不合法");
        }
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension != null && BLOCKED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BizException(400, "不允许上传可执行文件");
        }

        try {
            RequirementAttachment attachment = new RequirementAttachment();
            attachment.setRequirementId(requirementId);
            attachment.setFileName(fileName);
            attachment.setContentType(StringUtils.hasText(file.getContentType())
                    ? file.getContentType() : "application/octet-stream");
            attachment.setFileSize(file.getSize());
            attachment.setImage(attachment.getContentType().toLowerCase().startsWith("image/"));
            attachment.setFileData(file.getBytes());
            attachment.setUploadedBy(SecurityContextHolder.getContext().getAuthentication().getName());
            attachment.setUploadedAt(Instant.now());
            return attachmentRepository.save(attachment);
        } catch (IOException e) {
            throw new BizException(500, "读取上传文件失败");
        }
    }

    @Transactional
    public void delete(Long requirementId, Long attachmentId) {
        RequirementAttachment attachment = get(requirementId, attachmentId);
        attachmentRepository.delete(attachment);
    }

    private void ensureRequirement(Long requirementId) {
        if (!requirementRepository.existsById(requirementId)) {
            throw new BizException(404, "需求不存在");
        }
    }
}

