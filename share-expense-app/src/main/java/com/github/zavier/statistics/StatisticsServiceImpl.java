package com.github.zavier.statistics;

import com.alibaba.cola.catchlog.CatchAndLog;
import com.alibaba.cola.dto.SingleResponse;
import com.alibaba.cola.exception.Assert;
import com.github.zavier.api.StatisticsService;
import com.github.zavier.domain.expense.ExpenseProject;
import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.domain.expense.gateway.ExpenseProjectGateway;
import com.github.zavier.dto.data.statistics.PieStatisticsDTO;
import com.github.zavier.utils.FreemarkerUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CatchAndLog
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private ExpenseProjectGateway projectGateway;

    // 统计支出类型占比
    @Override
    public SingleResponse<String> statisticsByExpenseType(Integer projectId, Integer operatorId) {
        final Optional<ExpenseProject> projectOpt = projectGateway.getProjectById(projectId);
        Assert.isTrue(projectOpt.isPresent(), "项目不存在");
        final ExpenseProject project = projectOpt.get();

        Assert.isTrue(Objects.equals(project.getCreateUserId(), operatorId), "无权限");

        final List<ExpenseRecord> expenseRecords = project.listAllExpenseRecord();

        final String result = parsePieEChartConfig(expenseRecords);

        return SingleResponse.of(result);
    }


    private static String parsePieEChartConfig(List<ExpenseRecord> expenseRecords) {
        // 1. 数据处理，转换成百分比
        final Map<String, LongSummaryStatistics> map = expenseRecords.stream()
                .collect(Collectors.groupingBy(ExpenseRecord::getExpenseType, Collectors.summarizingLong(it -> it.getAmount().multiply(BigDecimal.valueOf(100)).longValue())));
        final Long total = map.values().stream().map(LongSummaryStatistics::getSum).reduce(0L, Long::sum);
        List<PieStatisticsDTO> list = new ArrayList<>();
        map.forEach((type, value) -> {
            final PieStatisticsDTO dto = new PieStatisticsDTO();
            final String percent = BigDecimal.valueOf(value.getSum())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_DOWN)
                    .toPlainString();
            dto.setLabel(type + "(" + percent + "%)");
            dto.setValue(value.getSum());
            list.add(dto);
        });

        // 模板渲染成 echars格式
        String templateName = "pieStatistics.ftl";
        Map<String, Object> data = new HashMap<>();
        data.put("title", "费用类型信息");
        data.put("dataList", list);

        return FreemarkerUtils.processTemplate(templateName, data);
    }
}
