package com.github.zavier.ai.function;

import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ProjectService;
import com.github.zavier.dto.ExpenseRecordQry;
import com.github.zavier.dto.ProjectListQry;
import com.github.zavier.dto.ProjectSharingQry;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.dto.data.ProjectDTO;
import com.github.zavier.dto.data.UserSharingDTO;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询项目费用明细与分析的工具方法
 */
@Slf4j
@Component
public class GetExpenseDetailsFunction {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private ProjectService projectService;

    /**
     * 根据项目名称查询项目的费用明细与分析（推荐使用）
     *
     * @param projectName 项目名称
     * @return 费用明细报告
     */
    @Tool(description = "根据项目名称查询项目的全部费用记录并进行汇总分析。返回包含总览统计、按类型分类、按成员汇总和费用明细的报告。")
    public String getExpenseDetailsByName(@ToolParam(description = "项目名称") String projectName) {

        log.info("[AI工具] 开始执行 getExpenseDetailsByName, 参数: projectName={}, userId={}", projectName, getCurrentUserId());

        // 先根据项目名称查找项目ID
        Integer projectId = findProjectIdByName(projectName);
        if (projectId == null) {
            log.warn("[AI工具] getExpenseDetailsByName 未找到项目: projectName={}", projectName);
            return "未找到名为 \"" + projectName + "\" 的项目，请先使用 listProjects 查看您的项目列表";
        }

        return doGetExpenseDetails(projectId);
    }

    /**
     * 根据项目ID查询项目的费用明细与分析
     *
     * @param projectId 项目ID
     * @return 费用明细报告
     */
    @Tool(description = "根据项目ID查询项目的全部费用记录并进行汇总分析。返回包含总览统计、按类型分类、按成员汇总和费用明细的报告。推荐优先使用 getExpenseDetailsByName 方法。")
    public String getExpenseDetails(@ToolParam(description = "项目ID") Integer projectId) {

        log.info("[AI工具] 开始执行 getExpenseDetails, 参数: projectId={}, userId={}", projectId, getCurrentUserId());
        return doGetExpenseDetails(projectId);
    }

    /**
     * 实际执行费用明细查询的方法
     */
    private String doGetExpenseDetails(Integer projectId) {
        // 查询费用记录
        ExpenseRecordQry qry = new ExpenseRecordQry();
        qry.setProjectId(projectId);
        qry.setOperatorId(getCurrentUserId());

        SingleResponse<List<ExpenseRecordDTO>> response = projectService.listRecord(qry);

        if (!response.isSuccess()) {
            log.error("[AI工具] getExpenseDetails 执行失败: {}", response.getErrMessage());
            return "查询费用记录失败: " + response.getErrMessage();
        }

        List<ExpenseRecordDTO> records = response.getData();

        if (records == null || records.isEmpty()) {
            log.info("[AI工具] getExpenseDetails 项目无费用记录: projectId={}", projectId);
            return "该项目暂无费用记录";
        }

        // 构建结构化数据返回给 AI 进行汇总分析
        String result = buildExpenseDetailsData(projectId, records);
        log.info("[AI工具] getExpenseDetails 执行成功, 项目ID={}, 费用记录数={}", projectId, records.size());
        return result;
    }

    /**
     * 构建费用明细数据，返回结构化文本供 AI 汇总分析
     */
    private String buildExpenseDetailsData(Integer projectId, List<ExpenseRecordDTO> records) {
        StringBuilder sb = new StringBuilder();

        // 总览信息
        BigDecimal totalAmount = records.stream()
                .map(ExpenseRecordDTO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 时间范围
        long minDate = records.stream()
                .mapToLong(ExpenseRecordDTO::getDate)
                .min()
                .orElse(0L);
        long maxDate = records.stream()
                .mapToLong(ExpenseRecordDTO::getDate)
                .max()
                .orElse(0L);

        String startDate = minDate > 0 ? formatDate(minDate) : "未知";
        String endDate = maxDate > 0 ? formatDate(maxDate) : "未知";

        // 涉及的成员（去重）
        List<String> allMembers = records.stream()
                .flatMap(r -> r.getConsumeMembers().stream())
                .distinct()
                .collect(Collectors.toList());

        sb.append(String.format("项目ID: %d 的费用明细数据：\n\n", projectId));
        sb.append("## 总览信息\n");
        sb.append(String.format("- 总支出: %.2f 元\n", totalAmount));
        sb.append(String.format("- 总笔数: %d 笔\n", records.size()));
        sb.append(String.format("- 涉及成员: %d 人 (%s)\n", allMembers.size(), String.join("、", allMembers)));
        sb.append(String.format("- 时间范围: %s 至 %s\n\n", startDate, endDate));

        // 按类型分类统计
        sb.append("## 按类型分类统计\n");
        records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getExpenseType() == null || r.getExpenseType().isEmpty() ? "未分类" : r.getExpenseType(),
                        Collectors.reducing(BigDecimal.ZERO, ExpenseRecordDTO::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .forEach(entry -> {
                    long count = records.stream()
                            .filter(r -> (r.getExpenseType() == null || r.getExpenseType().isEmpty()
                                    ? "未分类" : r.getExpenseType()).equals(entry.getKey()))
                            .count();
                    double percentage = entry.getValue().divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                    sb.append(String.format("- %s: %.2f 元 (%.1f%%), %d 笔\n",
                            entry.getKey(), entry.getValue(), percentage, count));
                });
        sb.append("\n");

        // 按成员汇总统计
        sb.append("## 按成员汇总统计\n");
        for (String member : allMembers) {
            BigDecimal paidAmount = records.stream()
                    .filter(r -> member.equals(r.getPayMember()))
                    .map(ExpenseRecordDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal consumeAmount = records.stream()
                    .filter(r -> r.getConsumeMembers().contains(member))
                    .map(r -> {
                        int consumerCount = r.getConsumeMembers().size();
                        return r.getAmount().divide(BigDecimal.valueOf(consumerCount), 2, BigDecimal.ROUND_HALF_UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long payCount = records.stream()
                    .filter(r -> member.equals(r.getPayMember()))
                    .count();

            long consumeCount = records.stream()
                    .filter(r -> r.getConsumeMembers().contains(member))
                    .count();

            BigDecimal netAmount = paidAmount.subtract(consumeAmount);

            sb.append(String.format("- %s: 付款 %.2f 元, 消费 %.2f 元, 净收支 %+.2f 元, 付款 %d 次, 参与 %d 次\n",
                    member, paidAmount, consumeAmount, netAmount, payCount, consumeCount));
        }
        sb.append("\n");

        // 费用明细
        sb.append("## 费用明细列表\n");
        for (int i = 0; i < records.size(); i++) {
            ExpenseRecordDTO record = records.get(i);
            sb.append(String.format("%d. 日期: %s, 付款人: %s, 金额: %.2f 元, 类型: %s, 备注: %s, 消费人员: %s\n",
                    i + 1,
                    formatDate(record.getDate()),
                    record.getPayMember(),
                    record.getAmount(),
                    record.getExpenseType() == null || record.getExpenseType().isEmpty() ? "未分类" : record.getExpenseType(),
                    record.getRemark() == null || record.getRemark().isEmpty() ? "无" : record.getRemark(),
                    String.join("、", record.getConsumeMembers())
            ));
        }

        sb.append("\n请根据以上数据生成一份 Markdown 格式的费用明细分析报告，包含：总览、按类型分类统计、按成员汇总统计、费用明细表格。");
        return sb.toString();
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

        var response = projectService.pageProject(qry);
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

    /**
     * 格式化日期（秒时间戳 -> yyyy-MM-dd）
     */
    private String formatDate(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "未知";
        }
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp),
                    ZoneId.systemDefault()
            );
            return dateTime.format(DATE_FORMATTER);
        } catch (Exception e) {
            return "未知";
        }
    }

    private Integer getCurrentUserId() {
        return UserHolder.getUser() != null ? UserHolder.getUser().getUserId() : 1;
    }
}
