package com.github.zavier.project.executor.converter;

import com.github.zavier.domain.expense.ExpenseRecord;
import com.github.zavier.project.executor.bo.ExpenseRecordExcelBO;
import com.google.common.base.Joiner;

import java.text.SimpleDateFormat;

public class ExpenseRecordConverter {

    private static String DATE_FORMAT = "yyyy-MM-dd";

    public static ExpenseRecordExcelBO convert(ExpenseRecord expenseRecord) {
        final ExpenseRecordExcelBO excelBO = new ExpenseRecordExcelBO();
        excelBO.setDate(new SimpleDateFormat(DATE_FORMAT).format(expenseRecord.getDate()));
        excelBO.setAmount(expenseRecord.getAmount().toPlainString());
        excelBO.setPayMember(expenseRecord.getPayMember());
        excelBO.setExpenseType(expenseRecord.getExpenseType());
        excelBO.setRemark(expenseRecord.getRemark());
        excelBO.setConsumers(Joiner.on(",").join(expenseRecord.listAllConsumers()));
        return excelBO;
    }
}
