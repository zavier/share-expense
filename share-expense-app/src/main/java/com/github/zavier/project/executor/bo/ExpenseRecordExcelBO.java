package com.github.zavier.project.executor.bo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ExpenseRecordExcelBO {
    @ExcelProperty("消费日期")
    private String date;
    @ExcelProperty("金额")
    private String amount;
    @ExcelProperty("付款人")
    private String payMember;
    @ExcelProperty("费用类型")
    private String expenseType;
    @ExcelProperty("备注")
    private String remark;
    @ExcelProperty("消费人")
    private String consumers;

    @ExcelIgnore
    private String projectName;
}
