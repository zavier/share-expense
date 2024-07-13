package com.github.zavier.dto;

import com.alibaba.cola.dto.Query;
import lombok.Data;

@Data
public class ExpenseRecordQry  extends Query {
    private Integer projectId;
    private Integer operatorId;
}
