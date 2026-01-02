package com.github.zavier.ai.dto;

/**
 * 费用查询响应格式枚举
 * <p>
 * 用于控制AI工具返回内容的详细程度，优化Token效率。
 * 基于 Anthropic 最佳实践：https://www.anthropic.com/engineering/writing-tools-for-agents
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
public enum ExpenseResponseFormat {

    /**
     * 精简模式 - 只返回核心信息
     * <p>
     * 特点：
     * <ul>
     *   <li>不包含技术ID（project_id, member_id等）</li>
     *   <li>不包含元数据（创建时间、更新时间等）</li>
     *   <li>返回自然语言描述</li>
     *   <li>Token消耗通常为详细模式的1/3</li>
     * </ul>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>用户直接查看结果（AI向用户展示）</li>
     *   <li>不需要进一步处理的场景</li>
     *   <li>优化Token消耗</li>
     * </ul>
     * <p>
     * 示例：
     * <pre>
     * # 周末聚餐 的结算情况
     *
     * • 张三：应收 100.00 元
     * • 李四：应付 50.00 元
     * • 王五：应付 50.00 元
     * </pre>
     */
    CONCISE,

    /**
     * 详细模式 - 包含完整信息
     * <p>
     * 特点：
     * <ul>
     *   <li>包含所有ID和技术字段</li>
     *   <li>包含元数据（创建时间、更新时间等）</li>
     *   <li>返回结构化数据</li>
     *   <li>便于后续工具调用</li>
     * </ul>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>AI需要进行后续处理（如调用其他工具）</li>
     *   <li>需要精确的ID进行操作</li>
     *   <li>需要完整的元数据</li>
     * </ul>
     * <p>
     * 示例：
     * <pre>
     * # 项目 5 结算详情
     *
     * ## 张三（ID: 101）
     * - 已付：200.00 元
     * - 消费：100.00 元
     * - 结算：100.00 元
     *
     * ## 李四（ID: 102）
     * - 已付：50.00 元
     * - 消费：100.00 元
     * - 结算：-50.00 元
     * </pre>
     */
    DETAILED
}
