package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.project.ExpenseProjectDO;

/**
 * ExpenseProjectDO 转换器
 * <p>
 * 注意：createdAt 和 updatedAt 字段由 JPA Auditing 自动填充，无需手动设置
 */
public class ExpenseProjectConverter {

    /**
     * 转换为插入 DO
     * <p>
     * JPA Auditing 会自动设置：
     * - createdAt: 当前时间
     * - updatedAt: 当前时间
     * - version: 初始值必须显式设置为 0
     * <p>
     * 重要：对于新实体，必须显式设置 version=0
     * @Version 注解会在更新时自动递增，但初始值需要手动设置
     */
    public static ExpenseProjectDO toInsertDO(ExpenseProject expenseProject) {
        final ExpenseProjectDO expenseProjectDO = new ExpenseProjectDO();
        expenseProjectDO.setId(expenseProject.getId());
        expenseProjectDO.setName(expenseProject.getName());
        expenseProjectDO.setDescription(expenseProject.getDescription());
        expenseProjectDO.setCreateUserId(expenseProject.getCreateUserId());
        expenseProjectDO.setLocked(false);
        // 重要：新实体必须显式设置 version=0
        // @Version 只在更新时自动递增，初始值需要手动设置
        // 否则在删除等操作时 JPA 会报错："Detached entity has uninitialized version value"
        expenseProjectDO.setVersion(0);
        // createdAt 和 updatedAt 由 JPA Auditing 自动填充，无需手动设置
        return expenseProjectDO;
    }

    /**
     * 转换为更新 DO
     * <p>
     * JPA Auditing 会自动更新：
     * - updatedAt: 当前时间
     * - version: @Version 注解自动递增
     * <p>
     * 注意：不要手动设置 version 字段，让 JPA 的 @Version 注解自动管理
     */
    public static ExpenseProjectDO toUpdateDO(ExpenseProject expenseProject) {
        final ExpenseProjectDO expenseProjectDO = new ExpenseProjectDO();
        expenseProjectDO.setId(expenseProject.getId());
        expenseProjectDO.setCreateUserId(expenseProject.getCreateUserId());
        expenseProjectDO.setName(expenseProject.getName());
        expenseProjectDO.setDescription(expenseProject.getDescription());
        expenseProjectDO.setLocked(expenseProject.getLocked());
        // 重要：不要设置 version 字段！
        // @Version 注解会让 JPA 自动：
        // 1. 检查版本号是否匹配
        // 2. 递增版本号
        // 3. 如果版本号不匹配，抛出 OptimisticLockException
        // updatedAt 由 JPA Auditing 自动更新，无需手动设置
        return expenseProjectDO;
    }
}
