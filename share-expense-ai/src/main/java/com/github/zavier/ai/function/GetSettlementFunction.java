package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 查询项目结算情况的工具方法
 */
@Component
public class GetSettlementFunction {

    @Resource
    private ProjectService projectService;

    /**
     * 查询项目的费用结算情况
     *
     * @param projectId 项目ID
     * @return 结算情况详情
     */
    @Tool(description = "查询项目的费用结算情况，显示每个人应付或应收的金额。需要提供项目ID。")
    public String getSettlement(@ToolParam(description = "项目ID") Integer projectId) {
        ProjectSharingQry qry = new ProjectSharingQry();
        qry.setProjectId(projectId);
        qry.setOperatorId(getCurrentUserId());

        SingleResponse<List<UserSharingDTO>> response = projectService.getProjectSharingDetail(qry);

        if (!response.isSuccess()) {
            return "查询结算失败: " + response.getErrMessage();
        }

        List<UserSharingDTO> settlements = response.getData();
        StringBuilder sb = new StringBuilder();
        sb.append("项目结算情况：\n");

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

        return sb.toString();
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
