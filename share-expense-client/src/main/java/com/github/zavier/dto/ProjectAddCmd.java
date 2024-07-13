package com.github.zavier.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectAddCmd {
    private String projectName;
    private String projectDesc;

    /**
     * 创建人
     */
    private Integer createUserId;
    private String createUserName;

    /**
     * 成员
     */
    private List<String> members = new ArrayList<>();
}
