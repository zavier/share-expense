package com.github.zavier.ai.function;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@AiFunction(
    name = "addExpenseRecord",
    description = "添加一笔费用记录。需要提供项目ID、付款人、金额、费用类型、消费日期（yyyy-MM-dd格式）、参与消费的成员列表。"
)
public class AddExpenseRecordFunction implements AiFunctionExecutor {

    @Resource
    private ProjectService projectService;

    public record Request(
        Integer projectId,
        String payer,
        BigDecimal amount,
        String expenseType,
        String payDate,
        List<String> consumers,
        String remark
    ) {}

    @Override
    public String execute(Object request, FunctionContext context) {
        Request req = (Request) request;
        ExpenseRecordAddCmd cmd = new ExpenseRecordAddCmd();
        cmd.setProjectId(req.projectId());
        cmd.setPayMember(req.payer());
        cmd.setAmount(req.amount());
        cmd.setExpenseType(req.expenseType());
        cmd.setRemark(req.remark());
        cmd.setOperatorId(context.getUserId());

        // 解析日期转换为时间戳（秒）
        LocalDate date = parseDate(req.payDate());
        long timestamp = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        cmd.setDate(timestamp);

        // 设置消费者成员列表
        cmd.setConsumerMembers(req.consumers());

        Response response = projectService.addExpenseRecord(cmd);

        if (!response.isSuccess()) {
            throw new RuntimeException("添加费用记录失败: " + response.getErrMessage());
        }

        return String.format("费用记录添加成功！付款人：%s，金额：%.2f元", req.payer(), req.amount());
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

    @Override
    public Class<Request> getRequestType() {
        return Request.class;
    }
}
