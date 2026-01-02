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
 * 添加费用记录的工具方法（v2.0优化版）
 * <p>
 * 主要优化：
 * <ul>
 *   <li>支持项目名称或ID自动识别</li>
 *   <li>增强参数验证：成员必须在项目中</li>
 *   <li>增强错误提示：提供具体的错误原因和建议</li>
 *   <li>详细的工具描述：包含使用场景和注意事项</li>
 * </ul>
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
@Component
public class ExpenseAddExpenseFunction extends BaseExpenseFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 添加一笔费用记录到指定项目。
     *
     * @param projectIdentifier 项目名称或项目ID
     * @param payer             付款人姓名，必须是项目成员
     * @param amount            金额，数字类型，单位元
     * @param expenseType       费用类型
     * @param consumers         参与消费的成员列表
     * @param payDate            消费日期（可选）
     * @param remark            备注说明（可选）
     * @return 添加结果
     */
    @Tool(description = """
            添加一笔费用记录到指定项目。

            参数说明：
            - project_identifier: 项目名称或项目ID（如"周末聚餐"或"5"）
            - payer: 付款人姓名，必须是项目成员
            - amount: 金额，数字类型，单位元（如100.50），必须大于0
            - expense_type: 费用类型，如"餐饮"、"交通"、"住宿"、"娱乐"等
            - consumers: 参与消费的成员列表，必须是项目成员，至少1人
            - pay_date: 消费日期（可选），格式yyyy-MM-dd（如2024-01-15），不填默认今天
            - remark: 备注说明（可选），记录消费的具体内容

            使用场景：
            - 用户说"记录一笔支出，Alice付了50元吃饭，我们3个人AA"
            - 用户说"添加交通费，Bob花了20元地铁"
            - 用户说"昨天Alice付的100元住宿费，4个人平摊"

            注意事项：
            - 付款人和所有消费成员必须在项目成员列表中
            - 金额必须大于0
            - 日期必须为yyyy-MM-dd格式或为空
            - consumers列表至少包含1人

            错误处理：
            - 如果项目不存在，会返回明确的错误提示
            - 如果成员不在项目中，会列出当前项目成员
            - 如果参数格式错误，会返回具体的格式说明
            """)
    public String addExpense(
            @ToolParam(description = "项目名称或项目ID") String projectIdentifier,
            @ToolParam(description = "付款人姓名，必须是项目成员") String payer,
            @ToolParam(description = "金额，数字类型，单位元，必须大于0") BigDecimal amount,
            @ToolParam(description = "费用类型，如餐饮、交通、住宿、娱乐等") String expenseType,
            @ToolParam(description = "参与消费的成员列表，必须是项目成员，至少1人") List<String> consumers,
            @ToolParam(description = "消费日期，格式yyyy-MM-dd，不填默认今天", required = false) String payDate,
            @ToolParam(description = "备注说明（可选）", required = false) String remark) {

        log.info("[AI工具] 开始执行 addExpense, 参数: projectIdentifier={}, payer={}, amount={}, expenseType={}, consumers={}, payDate={}, remark={}, userId={}",
                projectIdentifier, payer, amount, expenseType, consumers, payDate, remark, getCurrentUserId());

        // 1. 解析项目标识符
        Integer projectId = resolveProjectIdentifier(projectIdentifier);
        if (projectId == null) {
            log.warn("[AI工具] addExpense 未找到项目: projectIdentifier={}", projectIdentifier);
            return buildProjectNotFoundResponse(projectIdentifier);
        }

        // 2. 获取项目成员列表（用于验证）
        List<String> projectMembers = getProjectMembers(projectId);
        if (projectMembers.isEmpty()) {
            log.warn("[AI工具] addExpense 项目无成员: projectId={}", projectId);
            return "❌ 该项目暂无成员，请先使用 expense_add_members 添加成员";
        }

        // 3. 验证付款人
        if (!projectMembers.contains(payer)) {
            log.warn("[AI工具] addExpense 付款人不在项目中: payer={}, projectMembers={}", payer, projectMembers);
            return buildMemberNotFoundResponse("付款人", payer, projectMembers);
        }

        // 4. 验证消费成员列表
        if (consumers == null || consumers.isEmpty()) {
            log.warn("[AI工具] addExpense 消费成员列表为空");
            return buildMissingParamResponse("consumers（消费成员列表）");
        }

        List<String> invalidMembers = consumers.stream()
                .filter(member -> !projectMembers.contains(member))
                .toList();
        if (!invalidMembers.isEmpty()) {
            log.warn("[AI工具] addExpense 消费成员不在项目中: invalidMembers={}", invalidMembers);
            return buildMemberNotFoundResponse("消费成员", invalidMembers.get(0), projectMembers);
        }

        // 5. 验证金额
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[AI工具] addExpense 金额无效: amount={}", amount);
            return "❌ 金额必须大于0，请检查输入";
        }

        // 6. 解析日期
        LocalDate date = parseDate(payDate);
        if (date == null) {
            log.warn("[AI工具] addExpense 日期格式错误: payDate={}", payDate);
            return buildInvalidParamFormatResponse("pay_date", "yyyy-MM-dd（如 2024-01-15）或留空使用今天");
        }

        // 7. 构建命令对象
        ExpenseRecordAddCmd cmd = new ExpenseRecordAddCmd();
        cmd.setProjectId(projectId);
        cmd.setPayMember(payer);
        cmd.setAmount(amount);
        cmd.setExpenseType(expenseType);
        cmd.setRemark(remark);
        cmd.setOperatorId(getCurrentUserId());
        cmd.setConsumerMembers(consumers);

        // 转换日期为时间戳（秒）
        long timestamp = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        cmd.setDate(timestamp);

        // 8. 调用业务逻辑
        Response response = projectService.addExpenseRecord(cmd);

        if (!response.isSuccess()) {
            log.error("[AI工具] addExpense 执行失败: {}", response.getErrMessage());
            return "❌ 添加费用记录失败: " + response.getErrMessage();
        }

        String result = String.format("✅ 费用记录添加成功！\n\n- 付款人：%s\n- 金额：%.2f 元\n- 类型：%s\n- 消费成员：%s\n- 日期：%s",
                payer, amount, expenseType, String.join("、", consumers), date.toString());
        log.info("[AI工具] addExpense 执行成功: {}", result);
        return result;
    }

    /**
     * 解析日期
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("[AI工具] 日期解析失败: dateStr={}", dateStr);
            return null;
        }
    }
}
