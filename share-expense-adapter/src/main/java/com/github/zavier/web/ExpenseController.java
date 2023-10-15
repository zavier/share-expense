package com.github.zavier.web;

import com.alibaba.cola.dto.Response;
import com.alibaba.cola.dto.SingleResponse;
import com.github.zavier.api.ExpenseService;
import com.github.zavier.domain.user.User;
import com.github.zavier.dto.ExpenseRecordAddCmd;
import com.github.zavier.dto.ExpenseRecordQry;
import com.github.zavier.dto.ExpenseRecordSharingAddCmd;
import com.github.zavier.dto.data.ExpenseRecordDTO;
import com.github.zavier.vo.ResponseVo;
import com.github.zavier.vo.SingleResponseVo;
import com.github.zavier.web.filter.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/expense")
public class ExpenseController {

    @Resource
    private ExpenseService expenseService;

    @PostMapping("/addRecord")
    public ResponseVo saveExpenseRecord(@RequestBody ExpenseRecordAddCmd expenseRecordAddCmd) {
        final User user = UserHolder.getUser();
        expenseRecordAddCmd.setUserId(user.getUserId());
        final Response response = expenseService.addExpenseRecord(expenseRecordAddCmd);
        return ResponseVo.buildFromResponse(response);
    }

    @GetMapping("/listRecord")
    public SingleResponseVo<Map<String, List<ExpenseRecordDTO>>> listRecord(ExpenseRecordQry expenseRecordQry) {
        final SingleResponse<List<ExpenseRecordDTO>> listSingleResponse = expenseService.listRecord(expenseRecordQry);
        Map<String, List<ExpenseRecordDTO>> map = new HashMap<>();
        map.put("rows", listSingleResponse.getData());
        return SingleResponseVo.of(map);
    }

    @PostMapping("/addRecordSharing")
    public ResponseVo addExpenseRecordSharing(@RequestBody ExpenseRecordSharingAddCmd expenseRecordSharingAddCmd){
        final Response response = expenseService.addExpenseRecordSharing(expenseRecordSharingAddCmd);
        return ResponseVo.buildFromResponse(response);
    }
}
