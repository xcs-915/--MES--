package com.itrm.platform.service;

import com.itrm.platform.common.BizException;
import com.itrm.platform.domain.Notification;
import com.itrm.platform.repo.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final RestTemplate restTemplate;
    private final String wecomWebhook;
    public NotificationService(NotificationRepository repository, RestTemplateBuilder builder,
                               @Value("${integration.wecom.webhook:}") String wecomWebhook) {
        this.repository = repository;
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(5)).build();
        this.wecomWebhook = wecomWebhook;
    }
    @Transactional
    public void notifyUsers(Collection<String> recipients, Long requirementId, String eventType, String title, String content) {
        Set<String> unique = new LinkedHashSet<>();
        for (String recipient : recipients) if (StringUtils.hasText(recipient)) unique.add(recipient);
        String wecomStatus = pushWeCom(title, content);
        for (String recipient : unique) {
            Notification item = new Notification();
            item.setRecipient(recipient); item.setRequirementId(requirementId); item.setEventType(eventType);
            item.setTitle(title); item.setContent(content); item.setReadFlag(false); item.setWecomStatus(wecomStatus); item.setCreatedAt(Instant.now());
            repository.save(item);
        }
    }
    @Transactional(readOnly = true) public List<Notification> list(String recipient) { return repository.findTop50ByRecipientOrderByCreatedAtDesc(recipient); }
    @Transactional(readOnly = true) public long unread(String recipient) { return repository.countByRecipientAndReadFlagFalse(recipient); }
    @Transactional public Notification read(Long id, String recipient) {
        Notification item = repository.findById(id).orElseThrow(() -> new BizException(404, "消息不存在"));
        if (!recipient.equals(item.getRecipient())) throw new BizException(403, "无权操作该消息");
        item.setReadFlag(true); return repository.save(item);
    }
    private String pushWeCom(String title, String content) {
        if (!StringUtils.hasText(wecomWebhook)) return "NOT_CONFIGURED";
        try {
            Map<String, Object> markdown = new HashMap<>(); markdown.put("content", "**" + title + "**\n>" + content);
            Map<String, Object> body = new HashMap<>(); body.put("msgtype", "markdown"); body.put("markdown", markdown);
            restTemplate.postForObject(wecomWebhook, body, String.class); return "SENT";
        } catch (Exception ignored) { return "FAILED"; }
    }
}
