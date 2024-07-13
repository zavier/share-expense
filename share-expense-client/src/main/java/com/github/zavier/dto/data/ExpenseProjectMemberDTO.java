package com.github.zavier.dto.data;

import lombok.Data;

@Data
public class ExpenseProjectMemberDTO {
    /**
     * 所属费用项目ID
     */
    private Integer projectId;

    /**
     * 用户姓名
     */
    private String member;

}