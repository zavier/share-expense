package com.github.zavier.dto;

import lombok.Data;

@Data
public class ProjectDeleteCmd {
    private Integer projectId;
    private Integer operatorId;
}
