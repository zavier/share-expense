package com.github.zavier.web;

import com.alibaba.cola.dto.Response;
import com.github.zavier.api.ExpenseService;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import com.github.zavier.vo.ResponseVo;
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
    public ResponseVo saveExpenseRecord(@RequestBody ExpenseRecordAddCmd expenseRecordAddCmd) {
        final Response response = expenseService.addExpenseRecord(expenseRecordAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @PostMapping("/addRecordSharing")
    public ResponseVo addExpenseRecordSharing(@RequestBody ExpenseRecordSharingAddCmd expenseRecordSharingAddCmd){
        final Response response = expenseService.addExpenseRecordSharing(expenseRecordSharingAddCmd);
        return ResponseVo.buildFromResponse(response);
    }
}
