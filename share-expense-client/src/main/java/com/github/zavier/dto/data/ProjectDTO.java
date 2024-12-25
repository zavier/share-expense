package com.github.zavier.dto.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProjectDTO {
    private Integer projectId;
    private String projectName;
    private String projectDesc;

    private Integer totalMember;
    private BigDecimal totalExpense;

}
