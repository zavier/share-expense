package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 添加费用记录的工具方法
 */
@Slf4j
@Component
public class AddExpenseRecordFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 添加一笔费用记录
     *
     * @param projectId 项目ID
     * @param payer 付款人姓名
     * @param amount 金额
     * @param expenseType 费用类型
     * @param payDate 消费日期（yyyy-MM-dd格式，可选）
     * @param consumers 参与消费的成员列表
     * @param remark 备注（可选）
     * @return 添加结果消息
     */
    @Tool(description = "添加一笔费用记录。需要提供项目ID、付款人、金额、费用类型、参与消费的成员列表。用于在用户要记录费用时，进行持久化保存")
    public String addExpenseRecord(
            @ToolParam(description = "项目ID") Integer projectId,
            @ToolParam(description = "付款人姓名") String payer,
            @ToolParam(description = "金额") BigDecimal amount,
            @ToolParam(description = "费用类型") String expenseType,
            @ToolParam(description = "参与消费的成员列表") List<String> consumers,
            @ToolParam(description = "消费日期（yyyy-MM-dd格式）", required = false) String payDate,
            @ToolParam(description = "备注", required = false) String remark) {

        log.info("[AI工具] 开始执行 addExpenseRecord, 参数: projectId={}, payer={}, amount={}, expenseType={}, consumers={}, payDate={}, remark={}, userId={}",
            projectId, payer, amount, expenseType, consumers, payDate, remark, getCurrentUserId());

        ExpenseRecordAddCmd cmd = new ExpenseRecordAddCmd();
        cmd.setProjectId(projectId);
        cmd.setPayMember(payer);
        cmd.setAmount(amount);
        cmd.setExpenseType(expenseType);
        cmd.setRemark(remark);
        cmd.setOperatorId(getCurrentUserId());

        // 解析日期转换为时间戳（秒）
        LocalDate date = parseDate(payDate);
        long timestamp = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        cmd.setDate(timestamp);

        // 设置消费者成员列表
        cmd.setConsumerMembers(consumers);

        Response response = projectService.addExpenseRecord(cmd);

        if (!response.isSuccess()) {
            log.error("[AI工具] addExpenseRecord 执行失败: {}", response.getErrMessage());
            return "添加费用记录失败: " + response.getErrMessage();
        }

        String result = String.format("费用记录添加成功！付款人：%s，金额：%.2f元", payer, amount);
        log.info("[AI工具] addExpenseRecord 执行成功: {}", result);
        return result;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
