package com.github.zavier.api;

import com.alibaba.cola.dto.SingleResponse;

public interface StatisticsService {
    // 统计支出类型占比
    SingleResponse<String> statisticsByExpenseType(Integer projectId);
}
