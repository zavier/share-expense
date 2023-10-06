package com.github.zavier.web;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ExpenseService;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/expense")
public class ExpenseController {

    @Resource
    private ExpenseService expenseService;

    @PostMapping("/addRecord")
    public Response saveExpenseRecord(@RequestBody ExpenseRecordAddCmd expenseRecordAddCmd) {
        return expenseService.addExpenseRecord(expenseRecordAddCmd);
    }
}
