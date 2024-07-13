package com.github.zavier.web;


import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.StatisticsService;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expense")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @GetMapping("/project/statistics/expenseType")
    public SingleResponseVo getProjectSharingDetail(@RequestParam Integer projectId) {
        final SingleResponse<String> pieStatisticsDTOS = statisticsService.statisticsByExpenseType(projectId, UserHolder.getUser().getUserId());
        if (!pieStatisticsDTOS.isSuccess()) {
            return SingleResponseVo.buildFailure(pieStatisticsDTOS.getErrCode(), pieStatisticsDTOS.getErrMessage());
        }
        return SingleResponseVo.of(pieStatisticsDTOS.getData());
    }
}
