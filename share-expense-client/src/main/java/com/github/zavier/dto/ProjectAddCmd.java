package com.github.zavier.dto;

import lombok.Data;

@Data
public class ProjectAddCmd {
    private String projectName;
    private String projectDesc;

    /**
     * 创建人
     */
    private Integer userId;
    private String userName;

    /**
     * 其他成员名称(多个名称以逗号分隔)
     */
    private String members;
}
