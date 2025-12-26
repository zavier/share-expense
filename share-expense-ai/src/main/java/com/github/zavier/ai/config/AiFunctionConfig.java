package com.github.zavier.ai.config;

import com.github.zavier.ai.AiFunctionRegistry;
import com.github.zavier.ai.function.*;
import com.github.zavier.ai.function.FunctionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Spring AI Function Calling 配置
 * 将 AiFunctionExecutor 适配为 Spring AI 可用的 Function beans
 */
@Configuration
public class AiFunctionConfig {

    /**
     * 创建项目函数 - 创建费用分摊项目
     */
    @Bean("createProject")
    @Description("创建一个新的费用分摊项目。需要提供项目名称和成员列表。")
    public Function<CreateProjectFunction.Request, String> createProjectFunction(AiFunctionRegistry registry) {
        return request -> {
            AiFunctionExecutor executor = registry.getFunction("createProject");
            FunctionContext context = FunctionContext.builder()
                .userId(1) // TODO: 从实际用户上下文获取
                .build();
            return executor.execute(request, context);
        };
    }

    /**
     * 添加成员函数 - 向项目添加成员
     */
    @Bean("addMembers")
    @Description("向现有项目添加新成员。需要提供项目ID和成员名称列表。")
    public Function<AddMembersFunction.Request, String> addMembersFunction(AiFunctionRegistry registry) {
        return request -> {
            AiFunctionExecutor executor = registry.getFunction("addMembers");
            FunctionContext context = FunctionContext.builder()
                .userId(1) // TODO: 从实际用户上下文获取
                .build();
            return executor.execute(request, context);
        };
    }

    /**
     * 添加费用记录函数 - 记录费用支出
     */
    @Bean("addExpenseRecord")
    @Description("添加一笔费用记录。需要提供项目ID、付款人、金额、费用类型、消费日期（yyyy-MM-dd格式）、参与消费的成员列表。")
    public Function<AddExpenseRecordFunction.Request, String> addExpenseRecordFunction(AiFunctionRegistry registry) {
        return request -> {
            AiFunctionExecutor executor = registry.getFunction("addExpenseRecord");
            FunctionContext context = FunctionContext.builder()
                .userId(1) // TODO: 从实际用户上下文获取
                .build();
            return executor.execute(request, context);
        };
    }

    /**
     * 获取结算信息函数 - 查询结算情况
     */
    @Bean("getSettlement")
    @Description("查询项目的结算情况，包括每个成员应付和应收的金额。需要提供项目ID。")
    public Function<GetSettlementFunction.Request, String> getSettlementFunction(AiFunctionRegistry registry) {
        return request -> {
            AiFunctionExecutor executor = registry.getFunction("getSettlement");
            FunctionContext context = FunctionContext.builder()
                .userId(1) // TODO: 从实际用户上下文获取
                .build();
            return executor.execute(request, context);
        };
    }
}
