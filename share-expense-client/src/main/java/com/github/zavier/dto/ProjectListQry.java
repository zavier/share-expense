package com.github.zavier.dto;


import lombok.Data;

@Data
public class ProjectListQry {
    private Integer userId;

    private Integer page;

    private Integer size;
}
