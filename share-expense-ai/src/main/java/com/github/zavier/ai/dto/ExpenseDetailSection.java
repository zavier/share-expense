package com.github.zavier.ai.dto;

/**
 * 费用明细查询内容枚举
 * <p>
 * 用于控制 getExpenseDetails 工具返回的内容范围。
 * 基于 Anthropic 最佳实践中的"适度整合"原则：保持工具整合，通过参数控制返回内容。
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
public enum ExpenseDetailSection {

    /**
     * 只返回汇总统计
     * <p>
     * 包含内容：
     * <ul>
     *   <li>总览：总支出、总笔数、涉及成员数、时间范围</li>
     *   <li>按类型分类统计：各类型的支出金额和占比</li>
     *   <li>按成员汇总统计：各成员的付款金额、消费金额、净收支</li>
     * </ul>
     * <p>
     * 不包含：具体的费用记录列表
     * <p>
     * 使用场景：
     * <ul>
     *   <li>用户说"统计周末聚餐的总支出"</li>
     *   <li>用户说"看看每个人花了多少钱"</li>
     *   <li>用户说"按类型分析支出"</li>
     * </ul>
     * <p>
     * Token消耗：约50-80 tokens（精简模式）
     */
    SUMMARY,

    /**
     * 只返回明细记录列表
     * <p>
     * 包含内容：
     * <ul>
     *   <li>每笔费用的具体信息：日期、付款人、金额、类型、备注、消费人员</li>
     * </ul>
     * <p>
     * 不包含：汇总统计信息
     * <p>
     * 使用场景：
     * <ul>
     *   <li>用户说"查看周末聚餐的所有消费记录"</li>
     *   <li>用户说"列出每一笔支出"</li>
     *   <li>用户说"看看最近的花费明细"</li>
     * </ul>
     * <p>
     * Token消耗：约50-200 tokens（取决于page_size和response_format）
     */
    RECORDS,

    /**
     * 返回全部内容（汇总 + 明细）
     * <p>
     * 包含内容：
     * <ul>
     *   <li>汇总统计（同SUMMARY）</li>
     *   <li>明细记录列表（同RECORDS）</li>
     * </ul>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>用户说"查看周末聚餐的完整费用信息"</li>
     *   <li>用户说"给我一个全面的费用报告"</li>
     * </ul>
     * <p>
     * Token消耗：约100-300 tokens（取决于page_size和response_format）
     * <p>
     * 注意：如果用户只需要查看概况，建议优先使用SUMMARY模式以优化Token消耗
     */
    ALL
}
