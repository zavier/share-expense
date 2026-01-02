package com.github.zavier.converter;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.expense.ExpenseRecordConsumerDO;
import com.github.zavier.expense.ExpenseRecordDO;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * ExpenseRecordDO 转换器
 * <p>
 * 注意：createdAt 和 updatedAt 字段由 JPA Auditing 自动填充，无需手动设置
 * version 字段由 @Version 注解自动管理
 */
public class ExpenseRecordDoConverter {

    /**
     * 转换为插入 DO
     * <p>
     * JPA Auditing 会自动设置：
     * - createdAt: 当前时间
     * - updatedAt: 当前时间
     * - version: 初始值由 JPA 自动管理
     */
    public static ExpenseRecordDO toInsertExpenseRecordDO(ExpenseRecord expenseRecord) {
        final ExpenseRecordDO expenseRecordDO = new ExpenseRecordDO();
        expenseRecordDO.setPayMember(expenseRecord.getPayMember());
        expenseRecordDO.setProjectId(expenseRecord.getProjectId());
        expenseRecordDO.setAmount(expenseRecord.getAmount());
        // Date -> LocalDateTime 转换
        expenseRecordDO.setPayDate(convertToLocalDateTime(expenseRecord.getDate()));
        expenseRecordDO.setExpenseType(expenseRecord.getExpenseType());
        expenseRecordDO.setRemark(expenseRecord.getRemark());
        // createdAt 和 updatedAt 由 JPA Auditing 自动填充，无需手动设置
        return expenseRecordDO;
    }

    /**
     * 转换为 Domain 对象
     * <p>
     * LocalDateTime -> Date 转换
     */
    public static ExpenseRecord toExpenseRecord(ExpenseRecordDO expenseRecordDO, List<ExpenseRecordConsumerDO> consumerDOS) {
        final ExpenseRecord expenseRecord = new ExpenseRecord();
        expenseRecord.setId(expenseRecordDO.getId());
        expenseRecord.setProjectId(expenseRecordDO.getProjectId());
        expenseRecord.setPayMember(expenseRecordDO.getPayMember());
        expenseRecord.setAmount(expenseRecordDO.getAmount());
        // LocalDateTime -> Date 转换
        expenseRecord.setDate(convertToDate(expenseRecordDO.getPayDate()));
        expenseRecord.setExpenseType(expenseRecordDO.getExpenseType());
        expenseRecord.setRemark(expenseRecordDO.getRemark());

        if (CollectionUtils.isNotEmpty(consumerDOS)) {
            consumerDOS.stream()
                    .map(ExpenseRecordConsumerDO::getMember)
                    .forEach(expenseRecord::addConsumer);
        }
        return expenseRecord;
    }

    /**
     * Date 转换为 LocalDateTime
     */
    private static LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime 转换为 Date
     */
    private static Date convertToDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
