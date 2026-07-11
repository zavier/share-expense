package com.github.zavier.web;


import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.project.ExpenseApplicationService;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/expense")
public class StatisticsController {

    @Resource
    private ExpenseApplicationService expenseApplicationService;

    @GetMapping("/project/statistics/expenseType")
    public SingleResponseVo getProjectSharingDetail(@RequestParam Integer projectId) {
        final SingleResponse<String> pieStatisticsDTOS = expenseApplicationService.statisticsByExpenseType(projectId, UserHolder.getUser().getUserId());
        if (!pieStatisticsDTOS.isSuccess()) {
            return SingleResponseVo.buildFailure(pieStatisticsDTOS.getErrCode(), pieStatisticsDTOS.getErrMessage());
        }
        return SingleResponseVo.of(pieStatisticsDTOS.getData());
    }
}
