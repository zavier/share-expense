package com.github.zavier.dto;


import lombok.Data;

@Data
public class ProjectMemberAddCmd  {
    private Integer projectId;

    private String userNames;
}
