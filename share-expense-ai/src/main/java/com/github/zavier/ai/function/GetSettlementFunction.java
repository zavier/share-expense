package com.github.zavier.ai.function;

import com.alibaba.cola.dto.PageResponse;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.ProjectDTO;
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
 * 查询项目结算情况的工具方法
 */
@Slf4j
@Component
public class GetSettlementFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 根据项目名称查询项目的费用结算情况（推荐使用）
     *
     * @param projectName 项目名称
     * @return 结算情况详情
     */
    @Tool(description = "根据项目名称查询项目的费用结算情况，显示每个人应付或应收的金额。如果找到多个匹配项目，会返回第一个匹配的结果。")
    public String getSettlementByName(@ToolParam(description = "项目名称") String projectName) {

        log.info("[AI工具] 开始执行 getSettlementByName, 参数: projectName={}, userId={}", projectName, getCurrentUserId());

        // 先根据项目名称查找项目ID
        Integer projectId = findProjectIdByName(projectName);
        if (projectId == null) {
            log.warn("[AI工具] getSettlementByName 未找到项目: projectName={}", projectName);
            return "未找到名为 \"" + projectName + "\" 的项目，请先使用 listProjects 查看您的项目列表";
        }

        return doGetSettlement(projectId);
    }

    /**
     * 根据项目ID查询项目的费用结算情况
     *
     * @param projectId 项目ID
     * @return 结算情况详情
     */
    @Tool(description = "根据项目ID查询项目的费用结算情况，显示每个人应付或应收的金额。推荐优先使用 getSettlementByName 方法。")
    public String getSettlement(@ToolParam(description = "项目ID") Integer projectId) {

        log.info("[AI工具] 开始执行 getSettlement, 参数: projectId={}, userId={}", projectId, getCurrentUserId());
        return doGetSettlement(projectId);
    }

    /**
     * 实际执行结算查询的方法
     */
    private String doGetSettlement(Integer projectId) {
        ProjectSharingQry qry = new ProjectSharingQry();
        qry.setProjectId(projectId);
        qry.setOperatorId(getCurrentUserId());

        SingleResponse<List<UserSharingDTO>> response = projectService.getProjectSharingDetail(qry);

        if (!response.isSuccess()) {
            log.error("[AI工具] getSettlement 执行失败: {}", response.getErrMessage());
            return "查询结算失败: " + response.getErrMessage();
        }

        List<UserSharingDTO> settlements = response.getData();
        StringBuilder sb = new StringBuilder();
        sb.append("项目id: ").append(projectId).append(" 的结算情况：\n");

        for (UserSharingDTO settlement : settlements) {
            // 计算结算金额: 已付 - 已消费
            BigDecimal settlementAmount = settlement.getPaidAmount().subtract(settlement.getConsumeAmount());

            if (settlementAmount.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format("- %s 应收 %.2f 元（已付%.2f，消费%.2f）\n",
                    settlement.getMember(), settlementAmount, settlement.getPaidAmount(), settlement.getConsumeAmount()));
            } else if (settlementAmount.compareTo(BigDecimal.ZERO) < 0) {
                sb.append(String.format("- %s 应付 %.2f 元（已付%.2f，消费%.2f）\n",
                    settlement.getMember(), settlementAmount.abs(), settlement.getPaidAmount(), settlement.getConsumeAmount()));
            } else {
                sb.append(String.format("- %s 已结清（已付%.2f，消费%.2f）\n",
                    settlement.getMember(), settlement.getPaidAmount(), settlement.getConsumeAmount()));
            }
        }

        String result = sb.toString();
        log.info("[AI工具] getSettlement 执行成功, 项目ID={}, 成员数={}", projectId, settlements.size());
        return result;
    }

    /**
     * 根据项目名称查找项目ID
     */
    private Integer findProjectIdByName(String projectName) {
        ProjectListQry qry = new ProjectListQry();
        qry.setOperatorId(getCurrentUserId());
        qry.setName(projectName);
        qry.setPage(1);
        qry.setSize(50);

        PageResponse<ProjectDTO> response = projectService.pageProject(qry);
        if (!response.isSuccess() || response.getData().isEmpty()) {
            return null;
        }

        // 精确匹配优先
        for (ProjectDTO project : response.getData()) {
            if (project.getProjectName().equals(projectName)) {
                return project.getProjectId();
            }
        }

        // 模糊匹配（包含）
        for (ProjectDTO project : response.getData()) {
            if (project.getProjectName().contains(projectName)) {
                return project.getProjectId();
            }
        }

        // 返回第一个结果
        return response.getData().get(0).getProjectId();
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
