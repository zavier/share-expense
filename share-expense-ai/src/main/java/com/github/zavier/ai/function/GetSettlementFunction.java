package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.UserSharingDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AiFunction(
    name = "getSettlement",
    description = "查询项目的费用结算情况，显示每个人应付或应收的金额。需要提供项目ID。"
)
public class GetSettlementFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        Integer projectId
    ) {}

    @Override
    public String execute(Object request, FunctionContext context) {
        Request req = (Request) request;
        ProjectSharingQry qry = new ProjectSharingQry();
        qry.setProjectId(req.projectId());
        qry.setOperatorId(context.getUserId());

        SingleResponse<List<UserSharingDTO>> response = projectService.getProjectSharingDetail(qry);

        if (!response.isSuccess()) {
            throw new RuntimeException("查询结算失败: " + response.getErrMessage());
        }

        List<UserSharingDTO> settlements = response.getData();
        StringBuilder sb = new StringBuilder();
        sb.append("项目结算情况：\n");

        for (UserSharingDTO settlement : settlements) {
            // 计算结算金额: 已付 - 已消费
            java.math.BigDecimal settlementAmount = settlement.getPaidAmount().subtract(settlement.getConsumeAmount());

            if (settlementAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                sb.append(String.format("- %s 应收 %.2f 元（已付%.2f，消费%.2f）\n",
                    settlement.getMember(), settlementAmount, settlement.getPaidAmount(), settlement.getConsumeAmount()));
            } else if (settlementAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
                sb.append(String.format("- %s 应付 %.2f 元（已付%.2f，消费%.2f）\n",
                    settlement.getMember(), settlementAmount.abs(), settlement.getPaidAmount(), settlement.getConsumeAmount()));
            } else {
                sb.append(String.format("- %s 已结清（已付%.2f，消费%.2f）\n",
                    settlement.getMember(), settlement.getPaidAmount(), settlement.getConsumeAmount()));
            }
        }

        return sb.toString();
    }

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
