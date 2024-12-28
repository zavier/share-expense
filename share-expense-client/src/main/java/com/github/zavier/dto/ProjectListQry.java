package com.github.zavier.dto;


import lombok.Data;

@Data
public class ProjectListQry {
    private Integer operatorId;

    private Integer page;

    private Integer size;

    private String name;
}
