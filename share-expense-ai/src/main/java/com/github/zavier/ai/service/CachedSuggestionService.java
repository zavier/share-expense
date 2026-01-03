package com.github.zavier.ai.service;

import com.alibaba.ttl.threadpool.TtlExecutors;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 缓存建议服务
 * 提供同步获取建议的功能，支持缓存和并发控制
 *
 * 缓存策略（无过期时间）：
 * - Session 表：活跃缓存，用于快速读取，可以删除和更新
 * - Conversation 表（最后一条记录）：快照，用于历史记录和审计，只更新不删除
 * - 缓存失效：只在对话更新时清除，无时间过期限制
 *
 * 读取流程：
 * 1. 从 Session 表读取缓存（唯一读取来源）
 * 2. 如果有缓存直接返回，否则生成新建议
 *
 * 写入流程：
 * 1. 更新 Session 表（活跃缓存）
 * 2. 更新 Conversation 表最后一条记录（快照）
 *
 * 清除流程：
 * - 只清除 Session 表缓存，保留 Conversation 表快照
 * - 在对话更新（新增消息）时触发清除
 */
@Slf4j
@Service
public class CachedSuggestionService {

    private final ConversationRepository conversationRepository;
    private final AiSessionRepository sessionRepository;
    private final SuggestionGenerator suggestionGenerator;
    private final ObjectMapper objectMapper;
    private final LockManager lockManager;

    // 生成超时时间：30秒
    private static final long GENERATION_TIMEOUT_SECONDS = 30;

    // 使用 ConcurrentHashMap 存储每个会话的生成任务
    private final ConcurrentHashMap<String, CompletableFuture<List<SuggestionGenerator.SuggestionItem>>> generatingTasks
        = new ConcurrentHashMap<>();

    private Executor executor = TtlExecutors.getTtlExecutor(new ThreadPoolExecutor(10, 20, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20)));

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
     * 只从 Session 表读取缓存，Conversation 表仅作为快照存储
     * 无过期时间限制，只在对话更新时清除缓存
     *
     * @param conversationId 会话ID
     * @return 建议列表
     */
    public List<SuggestionGenerator.SuggestionItem> getSuggestionsSync(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            log.debug("No conversationId provided, returning default suggestions");
            return getDefaultSuggestions(true);
        }

        // 1. 尝试从 Session 表获取缓存
        Optional<AiSessionEntity> sessionOpt = sessionRepository.findByConversationId(conversationId);
        if (sessionOpt.isPresent()) {
            AiSessionEntity session = sessionOpt.get();

            // 如果有缓存，直接返回（无过期时间限制）
            if (StringUtils.isNotBlank(session.getLastSuggestions())) {
                log.debug("Returning cached suggestions from session for conversation {}", conversationId);
                return parseSuggestions(session.getLastSuggestions());
            }

            // 如果正在生成，等待生成完成
            if (Boolean.TRUE.equals(session.getSuggestionsGenerating())) {
                log.info("Suggestions are being generated, waiting for conversation {}", conversationId);
                return waitForGeneration(conversationId);
            }
        }

        // 2. 没有缓存，开始生成
        return generateSuggestionsSync(conversationId);
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

    public List<SuggestionGenerator.SuggestionItem> generateSuggestionsSync(String conversationId) {
        // 检查会话是否存在
        if (conversationRepository.countByConversationId(conversationId) == 0) {
            log.debug("No conversation found for conversationId {}, returning default suggestions", conversationId);
            return getDefaultSuggestions(true);
        }

        try (LockContext lockContext = lockManager.acquireLock(conversationId, 100, TimeUnit.MILLISECONDS)) {
            // 再次检查（双重检查锁定）- 防止在等待锁期间其他线程已经生成
            Optional<AiSessionEntity> sessionOpt = sessionRepository.findByConversationId(conversationId);
            if (sessionOpt.isPresent()) {
                AiSessionEntity session = sessionOpt.get();
                if (StringUtils.isNotBlank(session.getLastSuggestions())) {
                    return parseSuggestions(session.getLastSuggestions());
                }
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
                }, executor);

            // 保存任务引用，供其他请求等待
            generatingTasks.put(conversationId, task);

            try {
                // 同步等待生成完成（带超时）
                List<SuggestionGenerator.SuggestionItem> suggestions =
                    task.get(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                // 保存到数据库（Session表 + Conversation快照）
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

        // 如果没有找到任务，可能生成已经完成，重新从 Session 表查询
        return getSessionSuggestions(conversationId)
            .map(this::parseSuggestions)
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

    /**
     * 保存建议到数据库
     * - Session 表：更新活跃缓存（用于快速读取）
     * - Conversation 表（最后一条记录）：更新快照（用于历史记录）
     *
     * 注意：虽然保存了 suggestionsUpdatedAt 时间戳，但不用于缓存过期判断
     * 缓存只在对话更新时主动清除
     *
     * @param conversationId 会话ID
     * @param suggestions 建议列表
     */
    public void saveSuggestionsToDatabase(String conversationId,
                                          List<SuggestionGenerator.SuggestionItem> suggestions) {
        try {
            String json = formatSuggestions(suggestions);
            LocalDateTime now = LocalDateTime.now();

            // 更新 Session 表（活跃缓存）
            sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
                session.setLastSuggestions(json);
                session.setSuggestionsUpdatedAt(now); // 保留时间戳用于审计，但不用于过期判断
                session.setSuggestionsGenerating(false);
                sessionRepository.save(session);
                log.debug("Updated session cache for conversation {}", conversationId);
            });

            // 更新 Conversation 表最后一条记录（快照）
            getLastConversation(conversationId).ifPresent(conversation -> {
                conversation.setSuggestions(json);
                conversation.setSuggestionsUpdatedAt(now); // 保留时间戳用于审计
                conversationRepository.save(conversation);
                log.debug("Updated conversation snapshot for conversation {}", conversationId);
            });

        } catch (Exception e) {
            log.error("Failed to save suggestions to database", e);
        }
    }

    /**
     * 清除建议缓存（对话更新时调用）
     * 只清除 Session 表的活跃缓存，保留 Conversation 表的历史快照
     *
     * @param conversationId 会话ID
     */
    @Transactional
    public void clearSuggestionsCache(String conversationId) {
        try {
            // 只清除 Session 表缓存
            sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
                session.setLastSuggestions(null);
                session.setSuggestionsUpdatedAt(null);
                session.setSuggestionsGenerating(false);
                sessionRepository.save(session);
            });

            log.debug("Cleared session suggestions cache for conversation {} (conversation snapshot preserved)", conversationId);

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

    /**
     * 标记建议生成中
     * 在 Session 表设置生成标志
     */
    protected void markAsGenerating(String conversationId) {
        sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
            session.setSuggestionsGenerating(true);
            sessionRepository.save(session);
            log.debug("Marked suggestions as generating for conversation {}", conversationId);
        });
    }

    /**
     * 清除生成标志
     * 在 Session 表清除生成标志
     */
    protected void clearGeneratingFlag(String conversationId) {
        sessionRepository.findByConversationId(conversationId).ifPresent(session -> {
            session.setSuggestionsGenerating(false);
            sessionRepository.save(session);
            log.debug("Cleared generating flag for conversation {}", conversationId);
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
