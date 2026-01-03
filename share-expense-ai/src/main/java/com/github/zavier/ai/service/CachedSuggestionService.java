package com.github.zavier.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zavier.ai.concurrent.LockContext;
import com.github.zavier.ai.concurrent.LockManager;
import com.github.zavier.ai.entity.AiSessionEntity;
import com.github.zavier.ai.entity.ConversationEntity;
import com.github.zavier.ai.repository.AiSessionRepository;
import com.github.zavier.ai.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 缓存建议服务
 * 提供同步获取建议的功能，支持缓存和并发控制
 */
@Slf4j
@Service
public class CachedSuggestionService {

    private final ConversationRepository conversationRepository;
    private final AiSessionRepository sessionRepository;
    private final SuggestionGenerator suggestionGenerator;
    private final ObjectMapper objectMapper;
    private final LockManager lockManager;

    // 缓存有效期：5分钟
    private static final long CACHE_VALIDITY_MINUTES = 5;

    // 生成超时时间：30秒
    private static final long GENERATION_TIMEOUT_SECONDS = 30;

    // 使用 ConcurrentHashMap 存储每个会话的生成任务
    private final ConcurrentHashMap<String, CompletableFuture<List<SuggestionGenerator.SuggestionItem>>> generatingTasks
        = new ConcurrentHashMap<>();

    public CachedSuggestionService(ConversationRepository conversationRepository,
                                   AiSessionRepository sessionRepository,
                                   SuggestionGenerator suggestionGenerator,
                                   ObjectMapper objectMapper,
                                   LockManager lockManager) {
        this.conversationRepository = conversationRepository;
        this.sessionRepository = sessionRepository;
        this.suggestionGenerator = suggestionGenerator;
        this.objectMapper = objectMapper;
        this.lockManager = lockManager;
    }

    /**
     * 同步获取建议（核心方法）
     *
     * @param conversationId 会话ID
     * @return 建议列表
     */
    public List<SuggestionGenerator.SuggestionItem> getSuggestionsSync(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            log.debug("No conversationId provided, returning default suggestions");
            return getDefaultSuggestions(true);
        }

        // 1. 尝试从 Session 表获取缓存（优先级更高，因为包含完整的会话信息）
        Optional<String> sessionSuggestions = getSessionSuggestions(conversationId);
        if (sessionSuggestions.isPresent() && isCacheValid(
                sessionRepository.findByConversationId(conversationId)
                    .map(AiSessionEntity::getSuggestionsUpdatedAt)
                    .orElse(null))) {
            log.debug("Returning cached suggestions from session for conversation {}", conversationId);
            return parseSuggestions(sessionSuggestions.get());
        }

        // 2. 尝试从 Conversation 表获取缓存
        Optional<ConversationEntity> lastConversation = getLastConversation(conversationId);
        if (lastConversation.isPresent()) {
            ConversationEntity entity = lastConversation.get();

            // 如果有缓存且未过期，直接返回
            if (StringUtils.isNotBlank(entity.getSuggestions()) &&
                entity.getSuggestionsUpdatedAt() != null &&
                isCacheValid(entity.getSuggestionsUpdatedAt())) {
                log.debug("Returning cached suggestions for conversation {}", conversationId);
                return parseSuggestions(entity.getSuggestions());
            }

            // 如果正在生成，等待生成完成
            if (Boolean.TRUE.equals(entity.getSuggestionsGenerating())) {
                log.info("Suggestions are being generated, waiting for conversation {}", conversationId);
                return waitForGeneration(conversationId);
            }
        }

        // 3. 没有缓存或缓存过期，开始生成
        ConversationEntity conversationEntity = lastConversation.orElse(null);
        return generateSuggestionsSync(conversationId, conversationEntity);
    }

    /**
     * 从会话表获取缓存建议
     */
    private Optional<String> getSessionSuggestions(String conversationId) {
        return sessionRepository.findByConversationId(conversationId)
                .map(AiSessionEntity::getLastSuggestions)
                .filter(suggestions -> !suggestions.isEmpty())
                ;
    }

    /**
     * 获取会话的最后一条对话记录
     */
    private Optional<ConversationEntity> getLastConversation(String conversationId) {
        List<ConversationEntity> conversations = conversationRepository
            .findByConversationIdOrderByCreatedAtDesc(conversationId, Pageable.ofSize(1));
        return conversations.isEmpty() ? Optional.empty() : Optional.of(conversations.get(0));
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return false;
        }
        Duration age = Duration.between(updatedAt, LocalDateTime.now());
        // 使用 <= 确保正好5分钟时仍然有效
        return age.toSeconds() <= CACHE_VALIDITY_MINUTES * 60;
    }

    public List<SuggestionGenerator.SuggestionItem> generateSuggestionsSync(String conversationId,
                                                                             ConversationEntity entity) {
        if (entity == null) {
            log.debug("No conversation found for conversationId {}, returning default suggestions", conversationId);
            return getDefaultSuggestions(true);
        }

        try (LockContext lockContext = lockManager.acquireLock(conversationId, 100, TimeUnit.MILLISECONDS)) {
            // 再次检查（双重检查锁定）
            Optional<ConversationEntity> updatedEntity = getLastConversation(conversationId);
            if (updatedEntity.isPresent()) {
                ConversationEntity updated = updatedEntity.get();
                if (StringUtils.isNotBlank(updated.getSuggestions()) && isCacheValid(updated.getSuggestionsUpdatedAt())) {
                    return parseSuggestions(updated.getSuggestions());
                }
                entity = updated;
            }

            // 标记为生成中
            markAsGenerating(conversationId);

            // 创建生成任务
            CompletableFuture<List<SuggestionGenerator.SuggestionItem>> task =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return generateWithAI(conversationId);
                    } catch (Exception e) {
                        log.error("Failed to generate suggestions for conversation {}",
                                 conversationId, e);
                        return getDefaultSuggestions(isNewConversation(conversationId));
                    }
                });

            // 保存任务引用，供其他请求等待
            generatingTasks.put(conversationId, task);

            try {
                // 同步等待生成完成（带超时）
                List<SuggestionGenerator.SuggestionItem> suggestions =
                    task.get(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // 保存到数据库
                saveSuggestionsToDatabase(conversationId, suggestions);

                log.info("Successfully generated and cached suggestions for conversation {}",
                        conversationId);

                return suggestions;

            } catch (TimeoutException e) {
                log.error("Timeout generating suggestions for conversation {}", conversationId);
                // 清除生成标志
                clearGeneratingFlag(conversationId);
                return getDefaultSuggestions(isNewConversation(conversationId));

            } catch (Exception e) {
                log.error("Error waiting for suggestion generation", e);
                // 清除生成标志
                clearGeneratingFlag(conversationId);
                return getDefaultSuggestions(isNewConversation(conversationId));
            } finally {
                // 清理任务
                generatingTasks.remove(conversationId);
            }

        } catch (TimeoutException e) {
            // 获取锁超时，说明其他线程正在生成，等待其完成
            log.info("Timeout acquiring lock, waiting for existing generation task");
            return waitForGeneration(conversationId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock", e);
            return getDefaultSuggestions(isNewConversation(conversationId));
        }
    }

    /**
     * 等待其他线程的生成任务完成
     */
    private List<SuggestionGenerator.SuggestionItem> waitForGeneration(String conversationId) {
        CompletableFuture<List<SuggestionGenerator.SuggestionItem>> task = generatingTasks.get(conversationId);

        if (task != null) {
            try {
                return task.get(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.error("Timeout waiting for suggestion generation for conversation {}", conversationId);
                return getDefaultSuggestions(isNewConversation(conversationId));
            } catch (Exception e) {
                log.error("Error waiting for suggestion generation", e);
                return getDefaultSuggestions(isNewConversation(conversationId));
            }
        }

        // 如果没有找到任务，可能生成已经完成，重新从数据库查询
        return getSessionSuggestions(conversationId)
            .map(this::parseSuggestions)
            .or(() -> getLastConversation(conversationId)
                .map(ConversationEntity::getSuggestions)
                .map(this::parseSuggestions))
            .orElse(getDefaultSuggestions(isNewConversation(conversationId)));
    }

    /**
     * 调用 AI 生成建议
     */
    private List<SuggestionGenerator.SuggestionItem> generateWithAI(String conversationId) {
        // 获取对话历史
        List<ConversationEntity> history = conversationRepository
            .findByConversationIdOrderByCreatedAtAsc(conversationId);

        // 调用 SuggestionGenerator 生成建议
        return suggestionGenerator.generate(history, conversationId);
    }

    public void saveSuggestionsToDatabase(String conversationId,
                                          List<SuggestionGenerator.SuggestionItem> suggestions) {
        try {
            String json = formatSuggestions(suggestions);
            LocalDateTime now = LocalDateTime.now();

            // 更新 Session 表
            sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
                session.setLastSuggestions(json);
                session.setSuggestionsUpdatedAt(now);
                session.setSuggestionsGenerating(false);
                sessionRepository.save(session);
            });

            // 更新 Conversation 表（最后一条记录）
            getLastConversation(conversationId).ifPresent(conversation -> {
                conversation.setSuggestions(json);
                conversation.setSuggestionsUpdatedAt(now);
                conversation.setSuggestionsGenerating(false);
                conversationRepository.save(conversation);
            });

        } catch (Exception e) {
            log.error("Failed to save suggestions to database", e);
        }
    }

    /**
     * 清除建议缓存（对话更新时调用）
     */
    @Transactional
    public void clearSuggestionsCache(String conversationId) {
        try {
            // 清除 Session 表缓存
            sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
                session.setLastSuggestions(null);
                session.setSuggestionsUpdatedAt(null);
                session.setSuggestionsGenerating(false);
                sessionRepository.save(session);
            });

            // 清除 Conversation 表缓存
            getLastConversation(conversationId).ifPresent(conversation -> {
                conversation.setSuggestions(null);
                conversation.setSuggestionsUpdatedAt(null);
                conversation.setSuggestionsGenerating(false);
                conversationRepository.save(conversation);
            });

            log.debug("Cleared suggestions cache for conversation {}", conversationId);

        } catch (Exception e) {
            log.error("Failed to clear suggestions cache", e);
        }
    }

    /**
     * 判断是否为新会话（没有历史记录）
     */
    private boolean isNewConversation(String conversationId) {
        return conversationRepository.countByConversationId(conversationId) == 0;
    }

    /**
     * 解析 JSON 格式的建议
     */
    private List<SuggestionGenerator.SuggestionItem> parseSuggestions(String json) {
        if (json == null || json.isEmpty()) {
            return getDefaultSuggestions(true);
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<SuggestionGenerator.SuggestionItem>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse suggestions JSON", e);
            return getDefaultSuggestions(true);
        }
    }

    /**
     * 格式化建议为 JSON
     */
    private String formatSuggestions(List<SuggestionGenerator.SuggestionItem> suggestions) {
        try {
            return objectMapper.writeValueAsString(suggestions);
        } catch (JsonProcessingException e) {
            log.error("Failed to format suggestions to JSON", e);
            return "[]";
        }
    }

    protected void markAsGenerating(String conversationId) {
        getLastConversation(conversationId).ifPresent(entity -> {
            entity.setSuggestionsGenerating(true);
            conversationRepository.save(entity);
        });
    }

    protected void clearGeneratingFlag(String conversationId) {
        getLastConversation(conversationId).ifPresent(entity -> {
            entity.setSuggestionsGenerating(false);
            conversationRepository.save(entity);
        });
    }

    /**
     * 获取默认建议（降级方案）
     */
    private List<SuggestionGenerator.SuggestionItem> getDefaultSuggestions(boolean isNewUser) {
        List<SuggestionGenerator.SuggestionItem> suggestions = new ArrayList<>();

        if (isNewUser) {
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "创建项目「周末聚餐」，成员有小明、小红、小李", null, 1
            ));
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "今天午饭AA，80元4个人分,小明出钱", null, 1
            ));
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "查看我的项目", null, 1
            ));
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "查询「周末聚餐」的费用明细", null, 1
            ));
        } else {
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "查看我的项目列表", null, 1
            ));
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "记录一笔费用", null, 1
            ));
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "查看费用明细", null, 1
            ));
            suggestions.add(new SuggestionGenerator.SuggestionItem(
                "创建新项目", null, 1
            ));
        }

        return suggestions;
    }
}
