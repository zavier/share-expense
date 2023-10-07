package com.github.zavier.dto;


import lombok.Data;

@Data
public class ProjectListQry {
    /**
     * 项目成员用户ID
     */
    private Integer userId;

    private Integer page;

    private Integer size;
}
