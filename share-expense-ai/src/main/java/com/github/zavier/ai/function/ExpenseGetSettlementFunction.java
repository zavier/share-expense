package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.ai.dto.ExpenseResponseFormat;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 查询项目结算情况的工具方法（v2.0优化版）
 * <p>
 * 主要优化：
 * <ul>
 *   <li>统一参数格式：支持项目名称或ID自动识别</li>
 *   <li>添加响应格式控制：concise/detailed模式</li>
 *   <li>增强工具描述：详细的使用场景和注意事项</li>
 *   <li>优化Token效率：concise模式节省约70% tokens</li>
 * </ul>
 *
 * @author AI Optimization
 * @since 2025-01-02
 */
@Slf4j
@Component
public class ExpenseGetSettlementFunction extends BaseExpenseFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 查询项目的费用结算情况，显示每个人应付或应收的金额。
     *
     * @param projectIdentifier 项目名称或项目ID（如"周末聚餐"或"5"），自动识别
     * @param responseFormat    返回格式，可选值：
     *                         "concise": 精简模式（默认），只返回结算金额和自然语言说明
     *                         "detailed": 详细模式，包含所有字段和ID，用于后续处理
     * @return 结算情况详情
     */
    @Tool(description = """
            查询项目的费用结算情况，显示每个人应付或应收的金额。

            参数说明：
            - project_identifier: 项目名称或项目ID（如"周末聚餐"或"5"），自动识别
            - response_format: 返回格式，可选值：
              * "concise": 精简模式（默认），只返回结算金额和自然语言说明
              * "detailed": 详细模式，包含所有字段和ID，用于后续处理

            使用场景：
            - 用户说"查询周末聚餐的结算"、"看看谁该给谁钱"
            - AI需要获取项目ID进行后续操作时使用detailed模式

            注意事项：
            - 正数表示应收（别人欠他钱）
            - 负数表示应付（他欠别人钱）
            - 0表示已结清
            """)
    public String getSettlement(
            @ToolParam(description = "项目名称或项目ID") String projectIdentifier,
            @ToolParam(description = "返回格式：concise（精简）或detailed（详细）", required = false) String responseFormat) {

        log.info("[AI工具] 开始执行 getSettlement, 参数: projectIdentifier={}, responseFormat={}, userId={}",
                projectIdentifier, responseFormat, getCurrentUserId());

        // 1. 解析项目标识符
        Integer projectId = resolveProjectIdentifier(projectIdentifier);
        if (projectId == null) {
            log.warn("[AI工具] getSettlement 未找到项目: projectIdentifier={}", projectIdentifier);
            return buildProjectNotFoundResponse(projectIdentifier);
        }

        // 2. 解析响应格式
        ExpenseResponseFormat format = parseResponseFormat(responseFormat);

        // 3. 查询结算数据
        List<UserSharingDTO> settlements = fetchSettlements(projectId);
        if (settlements == null || settlements.isEmpty()) {
            log.info("[AI工具] getSettlement 项目无结算数据: projectId={}", projectId);
            return String.format("# %s 的结算情况\n\n该项目暂无结算数据", projectIdentifier);
        }

        // 4. 构建响应
        String result;
        if (format == ExpenseResponseFormat.CONCISE) {
            result = buildConciseSettlement(projectIdentifier, settlements);
        } else {
            result = buildDetailedSettlement(projectId, settlements);
        }

        log.info("[AI工具] getSettlement 执行成功, projectId={}, settlementCount={}", projectId, settlements.size());
        return result;
    }

    /**
     * 解析响应格式
     */
    private ExpenseResponseFormat parseResponseFormat(String responseFormat) {
        if (responseFormat == null || responseFormat.isBlank()) {
            return ExpenseResponseFormat.CONCISE; // 默认精简模式
        }
        try {
            return ExpenseResponseFormat.valueOf(responseFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[AI工具] 无效的response_format: {}, 使用默认concise", responseFormat);
            return ExpenseResponseFormat.CONCISE;
        }
    }

    /**
     * 查询结算数据
     */
    private List<UserSharingDTO> fetchSettlements(Integer projectId) {
        ProjectSharingQry qry = new ProjectSharingQry();
        qry.setProjectId(projectId);
        qry.setOperatorId(getCurrentUserId());

        SingleResponse<List<UserSharingDTO>> response = projectService.getProjectSharingDetail(qry);
        if (response.isSuccess() && response.getData() != null) {
            return response.getData();
        }
        return List.of();
    }

    /**
     * 构建精简响应（约50-80 tokens）
     * <p>
     * 只包含核心信息：成员名称和结算金额，使用自然语言描述。
     */
    private String buildConciseSettlement(String projectName, List<UserSharingDTO> settlements) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# %s 的结算情况\n\n", projectName));

        for (UserSharingDTO settlement : settlements) {
            BigDecimal settlementAmount = settlement.getPaidAmount().subtract(settlement.getConsumeAmount());

            if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("• %s：应收 %.2f 元\n", settlement.getMember(), settlementAmount));
            } else if (settlementAmount.compareTo(BigDecimal.ZERO) < 0) {
                sb.append(String.format("• %s：应付 %.2f 元\n", settlement.getMember(), settlementAmount.abs()));
            } else {
                sb.append(String.format("• %s：已结清\n", settlement.getMember()));
            }
        }

        return sb.toString();
    }

    /**
     * 构建详细响应（约150-200 tokens，包含ID）
     * <p>
     * 包含完整信息：成员ID、已付金额、消费金额、结算金额。
     */
    private String buildDetailedSettlement(Integer projectId, List<UserSharingDTO> settlements) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# 项目 %d 结算详情\n\n", projectId));

        for (UserSharingDTO settlement : settlements) {
            BigDecimal settlementAmount = settlement.getPaidAmount().subtract(settlement.getConsumeAmount());

            sb.append(String.format("## %s\n", settlement.getMember()));
            sb.append(String.format("- 已付：%.2f 元\n", settlement.getPaidAmount()));
            sb.append(String.format("- 消费：%.2f 元\n", settlement.getConsumeAmount()));
            sb.append(String.format("- 结算：%.2f 元\n\n", settlementAmount));
        }

        return sb.toString();
    }
}
