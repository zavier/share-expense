package com.github.zavier.dto;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectMemberAddCmd  {
    private Integer projectId;

    private List<String> members = new ArrayList<>();
}
