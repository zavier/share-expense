package com.github.zavier.dto;

import lombok.Data;

@Data
public class ProjectAddCmd {
    private String projectName;
    private String projectDesc;

    private Integer operatorId;
}
